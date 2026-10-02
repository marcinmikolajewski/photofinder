package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class CloudFlareAiCaller {

    @Value("${cloudflare.user.id}")
    private String userId;

    @Value("${cloudflare.model.name:@cf/llava-hf/llava-1.5-7b-hf}")
    private String MODEL;
    private final String uri = "v1/" + userId + "/photo-finder/workers-ai/" + MODEL;

    private final RestClient restClient;

    public CloudFlareAiCaller(@Qualifier("cloudFlare") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {HttpClientErrorException.class})
    public ResponseAiDto callModel(byte[] imageData, String prompt) {

        AiRequest body = new AiRequest();
        List<Integer> imageList = new ArrayList<>(imageData.length);
        for (byte b : imageData) {
            imageList.add(b & 0xFF);
        }
        body.image = imageList;
        body.prompt = prompt;
        body.max_tokens = 512;

        ResponseEntity<AiResponse> response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(AiResponse.class);

        return new ResponseAiDto(
                Optional.of(response)
                        .map(HttpEntity::getBody)
                        .map(it -> it.result)
                        .map(it -> it.description)
                        .orElse(""),
                Optional.of(response)
                        .map(HttpEntity::getHeaders)
                        .map(HttpHeaders::getContentLength)
                        .orElse(0L),
                MODEL,
                Status.DESCRIBED);
    }

    @Recover
    public ResponseAiDto recover(Exception e, byte[] imageData, String prompt) {
        log.error("Retry exhausted / non-retryable error for CLOUDFLARE API! Prompt: {}", prompt, e);
        return new ResponseAiDto("", 0L, MODEL, Status.ERROR);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AiRequest {
        public List<Integer> image;
        public String prompt;
        public Integer max_tokens;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AiResponse {
        public Result result;
        public boolean success;
        public List<Object> errors;

        public List<Object> messages;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Result {
            public String description;
            private long tokens;
        }
    }
}