package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto.GoogleAiResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GeminiAiCallerTest {

    @Test
    void callModel_givenValidResponse_whenInvoked_thenCallsRateLimiterAndReturnsModel() {
        // given
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        GeminiRateLimiter limiter = mock(GeminiRateLimiter.class);
        GeminiAiCaller caller = new GeminiAiCaller(restClient, limiter);
        setModel(caller, "gemini-2.0-flash");

        GoogleAiResponseDto dto = new GoogleAiResponseDto();
        GoogleAiResponseDto.Candidate candidate = new GoogleAiResponseDto.Candidate();
        GoogleAiResponseDto.Content content = new GoogleAiResponseDto.Content();
        GoogleAiResponseDto.Part part = new GoogleAiResponseDto.Part();
        part.setText("a description");
        content.setParts(java.util.List.of(part));
        candidate.setContent(content);
        dto.setCandidates(java.util.List.of(candidate));
        GoogleAiResponseDto.UsageMetadata usage = new GoogleAiResponseDto.UsageMetadata();
        usage.setTotalTokenCount(77);
        dto.setUsageMetadata(usage);

        when(restClient.post()
                .uri(anyString())
                .contentType(any(MediaType.class))
                .body(any())
                .retrieve()
                .toEntity(eq(GoogleAiResponseDto.class)))
            .thenReturn(ResponseEntity.ok(dto));

        byte[] image = new byte[]{1, 2, 3};
        String prompt = "Describe this photo";

        // when
        ResponseAiDto result = caller.callModel(image, prompt);

        // then
        verify(limiter, times(1)).acquire();
        assertThat(result.model()).isEqualTo("gemini-2.0-flash");
        assertThat(result.status()).isEqualTo(Status.DESCRIBED);
    }

    @Test
    void recover_givenRetriesExhausted_whenCalled_thenReturnsErrorResponse() {
        // given
        GeminiAiCaller caller = new GeminiAiCaller(mock(RestClient.class), mock(GeminiRateLimiter.class));
        setModel(caller, "gemini-2.0-flash");
        byte[] image = new byte[]{1};

        // when
        ResponseAiDto result = caller.recover(new RuntimeException("429"), image, "p");

        // then
        assertThat(result.response()).isEqualTo("");
        assertThat(result.totalToken()).isEqualTo(0L);
        assertThat(result.model()).isEqualTo("gemini-2.0-flash");
        assertThat(result.status()).isEqualTo(Status.ERROR);
    }

    private static void setModel(GeminiAiCaller caller, String model) {
        try {
            java.lang.reflect.Field f = GeminiAiCaller.class.getDeclaredField("MODEL");
            f.setAccessible(true);
            f.set(caller, model);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to set Gemini MODEL for tests", e);
        }
    }
}
