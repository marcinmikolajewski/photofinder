package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static eu.mm.software.photofinder.photosattribute.infrastructure.ai.AbstractAiService.TEMPERATURE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaAiCaller {

    @Value("${ollama.model.name:llava:13b}")
    private String MODEL;

    private final OllamaChatModel chatModel;

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 500, multiplier = 2),
            noRetryFor = {HttpClientErrorException.class})
    public ResponseAiDto callModel(byte[] imageData, String prompt) {
        try {
            Resource resource = new ByteArrayResource(imageData);

            Media media = new Media(MimeTypeUtils.IMAGE_JPEG, resource);

            var userMessage = UserMessage.builder()
                    .text(prompt)
                    .media(List.of(media))
                    .build();

            ChatResponse response = chatModel.call(
                    new Prompt(List.of(userMessage), OllamaChatOptions.builder()
                            .model(MODEL)
                            .temperature(TEMPERATURE)
                            .build()));

            return new ResponseAiDto(
                    Optional.of(response)
                            .map(ChatResponse::getResults)
                            .map(Collection::stream)
                            .flatMap(Stream::findAny)
                            .map(Generation::getOutput)
                            .map(AbstractMessage::getText)
                            .orElse(""),
                    Optional.of(response)
                            .map(ChatResponse::getMetadata)
                            .map(ChatResponseMetadata::getUsage)
                            .map(Usage::getTotalTokens)
                            .map(Integer::longValue)
                            .orElse(0L),
                    MODEL,
                    Status.DESCRIBED);
        } catch (Exception e) {
            log.error("Exception during call model: {}", e.getMessage());
            return new ResponseAiDto("", 0L, MODEL, Status.ERROR);
        }
    }

    @Recover
    public ResponseAiDto recover(Exception e, byte[] imageData, String prompt) {
        log.error("Retry exhausted for OLLAMA API! Prompt: {}", prompt, e);
        return new ResponseAiDto("", 0L, MODEL, Status.ERROR);
    }
}
