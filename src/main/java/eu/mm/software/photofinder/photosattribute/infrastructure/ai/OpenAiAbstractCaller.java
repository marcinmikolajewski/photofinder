package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.OpenAiBodyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.Optional;

import static eu.mm.software.photofinder.photosattribute.infrastructure.ai.AbstractAiService.TEMPERATURE;
import static eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.OpenAiBodyDto.createBody;

@Slf4j
@RequiredArgsConstructor
public abstract class OpenAiAbstractCaller {

    private final RestClient restClient;

    protected abstract String getModel();

    protected abstract String getUri();

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 500, multiplier = 2),
            noRetryFor = {HttpClientErrorException.class})
    public ResponseAiDto callModel(byte[] imageData, String prompt) {

        OpenAiBodyDto body = createBody(getModel(), prompt, imageData, TEMPERATURE);

        ResponseEntity<OpenAiApi.ChatCompletion> response = restClient.post()
                .uri(getUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(OpenAiApi.ChatCompletion.class);

        return new ResponseAiDto(
                Optional.ofNullable(response)
                        .map(HttpEntity::getBody)
                        .map(OpenAiApi.ChatCompletion::choices)
                        .stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(OpenAiApi.ChatCompletion.Choice::message)
                        .map(OpenAiApi.ChatCompletionMessage::content)
                        .orElse(""),
                Optional.ofNullable(response)
                        .map(HttpEntity::getBody)
                        .map(OpenAiApi.ChatCompletion::usage)
                        .map(OpenAiApi.Usage::totalTokens)
                        .map(Integer::longValue)
                        .orElse(0L),
                getModel(),
                Status.DESCRIBED
        );
    }

    @Recover
    public ResponseAiDto recover(Exception e, byte[] imageData, String prompt) {
        log.error("Retry exhausted / non-retryable error for {} API! Prompt: {}", getModel(), prompt, e);
        return new ResponseAiDto("", 0L, getModel(), Status.ERROR);
    }
}
