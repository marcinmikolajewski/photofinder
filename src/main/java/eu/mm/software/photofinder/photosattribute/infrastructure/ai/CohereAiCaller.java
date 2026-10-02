package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import com.cohere.api.Cohere;
import com.cohere.api.resources.v2.requests.V2ChatRequest;
import com.cohere.api.resources.v2.types.V2ChatResponse;
import com.cohere.api.types.*;
import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Base64;
import java.util.List;

import static eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.OpenAiBodyDto.PREFIX;

@Component
@Slf4j
@RequiredArgsConstructor
public class CohereAiCaller {

    @Value("${cohere.model.name:c4ai-aya-vision-8b}")
    private String MODEL;

    private final Cohere cohere;

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 500, multiplier = 2),
            noRetryFor = {HttpClientErrorException.class})
    public ResponseAiDto callModel(byte[] imageData, String prompt) {

        ImageUrl imageUrl = ImageUrl.builder()
                .url(PREFIX + Base64.getEncoder().encodeToString(imageData))
                .build();

        ChatTextContent chatTextContent = ChatTextContent.builder()
                .text(prompt)
                .build();
        ImageContent imageContent = ImageContent.builder()
                .imageUrl(imageUrl).build();

        List<Content> contents = List.of(Content.text(chatTextContent),
                Content.imageUrl(imageContent));
        UserMessageV2 userMessageV2 = UserMessageV2.builder()
                .content(UserMessageV2Content.of(contents))
                .build();
        V2ChatResponse chat = cohere.v2()
                .chat(
                        V2ChatRequest.builder()
                                .model(MODEL)
                                .messages(List.of(ChatMessageV2.user(userMessageV2)))
                                .build());

        String responseText = chat.getMessage().getContent()
                .map(List::getFirst)
                .flatMap(AssistantMessageResponseContentItem::getText)
                .map(ChatTextContent::getText)
                .orElse(StringUtils.EMPTY);

        Long token = chat.getUsage()
                .flatMap(Usage::getTokens)
                .flatMap(UsageTokens::getInputTokens)
                .map(Double::longValue)
                .orElse(0L);

        return new ResponseAiDto(
                responseText,
                token,
                MODEL,
                Status.DESCRIBED);
    }

    @Recover
    public ResponseAiDto recover(Exception e, byte[] imageData, String prompt) {
        log.error("Retry exhausted for COHERE API (429). Prompt: {}", prompt, e);
        return new ResponseAiDto("", 0L, MODEL, Status.ERROR);
    }
}
