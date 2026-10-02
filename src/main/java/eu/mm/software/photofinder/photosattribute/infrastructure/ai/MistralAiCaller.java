package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.OpenAiBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.Optional;

import static eu.mm.software.photofinder.photosattribute.infrastructure.ai.AbstractAiService.TEMPERATURE;
import static eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.OpenAiBodyDto.createBody;

@Slf4j
@Component
public class MistralAiCaller {

    @Value("${mistral.model.name:pixtral-12b-2409}")
    private String MODEL;

    private final RestClient restClient;

    public MistralAiCaller(@Qualifier("mistral") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 500, multiplier = 2),
            noRetryFor = {HttpClientErrorException.class})
    public ResponseAiDto callModel(byte[] imageData, String prompt) {

        OpenAiBodyDto body = createBody(MODEL, prompt, imageData, TEMPERATURE);

        ResponseEntity<MistralAiApi.ChatCompletion> response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(MistralAiApi.ChatCompletion.class);

        return new ResponseAiDto(
                Optional.of(response)
                        .map(HttpEntity::getBody)
                        .map(MistralAiApi.ChatCompletion::choices)
                        .stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(MistralAiApi.ChatCompletion.Choice::message)
                        .map(MistralAiApi.ChatCompletionMessage::content)
                        .orElse(""),
                Optional.of(response)
                        .map(HttpEntity::getBody)
                        .map(MistralAiApi.ChatCompletion::usage)
                        .map(MistralAiApi.Usage::completionTokens)
                        .map(Integer::longValue)
                        .orElse(0L),
                MODEL,
                Status.DESCRIBED);
    }

    @Recover
    public ResponseAiDto recover(Exception e, byte[] imageData, String prompt) {
        log.error("Retry exhausted / non-retryable error for MISTRAL API! Prompt: {}", prompt, e);
        return new ResponseAiDto("", 0L, MODEL, Status.ERROR);
    }
}
