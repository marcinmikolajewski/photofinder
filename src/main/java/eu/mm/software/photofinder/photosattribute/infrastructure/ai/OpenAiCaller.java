package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
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

import java.util.List;

import static eu.mm.software.photofinder.photosattribute.infrastructure.ai.AbstractAiService.TEMPERATURE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiCaller {

    @Value("${openai.model.name:gpt-4o-mini}")
    private String MODEL;

    private final OpenAiChatModel chatModel;

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {HttpClientErrorException.class})
    public ResponseAiDto callModel(byte[] imageData, String prompt) {

        Resource resource = new ByteArrayResource(imageData);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, resource);

        var userMessage = UserMessage.builder()
                .text(prompt)
                .media(List.of(media))
                .build();

        ChatResponse response = chatModel.call(
                new Prompt(List.of(userMessage), OpenAiChatOptions.builder()
                        .model(MODEL)
                        .temperature(TEMPERATURE)
                        .build()));

        return new ResponseAiDto(response.getResult().getOutput().getText(),
                response.getMetadata().getUsage().getTotalTokens().longValue(),
                MODEL,
                Status.DESCRIBED);
    }

    @Recover
    public ResponseAiDto recover(Exception e, byte[] imageData, String prompt) {
        log.error("Retry exhausted / non-retryable error for OPENAI API! Prompt: {}", prompt, e);
        return new ResponseAiDto("", 0L, MODEL, Status.ERROR);
    }
}
