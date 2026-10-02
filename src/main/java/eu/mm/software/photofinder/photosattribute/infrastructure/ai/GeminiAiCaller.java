package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import com.google.common.collect.Lists;
import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.GoogleAiBodyDto;
import eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.GoogleAiResponseDto;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
public class GeminiAiCaller {

    @Value("${gemini.model.name:gemini-2.0-flash}")
    private String MODEL;

    private final RestClient restClient;
    private final GeminiRateLimiter rateLimiter;

    public GeminiAiCaller(@Qualifier("gemini") RestClient restClient, GeminiRateLimiter rateLimiter) {
        this.restClient = restClient;
        this.rateLimiter = rateLimiter;
    }

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {HttpClientErrorException.class}
    )
    public ResponseAiDto callModel(byte[] imageData, String prompt) {

        rateLimiter.acquire();
        GoogleAiBodyDto body = new GoogleAiBodyDto();
        GoogleAiBodyDto.Content content = new GoogleAiBodyDto.Content();
        GoogleAiBodyDto.Part partImage = new GoogleAiBodyDto.Part();
        GoogleAiBodyDto.Part parttext = new GoogleAiBodyDto.Part();
        GoogleAiBodyDto.InlineData inlineData = new GoogleAiBodyDto.InlineData();
        inlineData.setData(Base64.getEncoder().encodeToString(imageData));
        inlineData.setMimeType("image/jpeg");
        parttext.setText(prompt);
        partImage.setInlineData(inlineData);

        content.setParts(Lists.newArrayList(partImage, parttext));
        body.setContents(Lists.newArrayList(content));

        ResponseEntity<GoogleAiResponseDto> response = restClient.post()
                .uri("v1beta/models/gemini-2.0-flash:generateContent")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(GoogleAiResponseDto.class);

        return new ResponseAiDto(
                Optional.of(response)
                        .map(HttpEntity::getBody)
                        .map(GoogleAiResponseDto::getCandidates)
                        .stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(GoogleAiResponseDto.Candidate::getContent)
                        .map(GoogleAiResponseDto.Content::getParts)
                        .map(Collection::stream)
                        .flatMap(Stream::findAny)
                        .map(GoogleAiResponseDto.Part::getText)
                        .orElse(""),
                Optional.of(response)
                        .map(HttpEntity::getBody)
                        .map(GoogleAiResponseDto::getUsageMetadata)
                        .map(GoogleAiResponseDto.UsageMetadata::getTotalTokenCount)
                        .map(Integer::longValue)
                        .orElse(0L),
                MODEL,
                Status.DESCRIBED);
    }

    @Recover
    public ResponseAiDto recover(Exception e, byte[] imageData, String prompt) {
        log.error("Retry exhausted for GEMINI API (429). Prompt: {}", prompt, e);

        return new ResponseAiDto("", 0L, MODEL, Status.ERROR);
    }
}
