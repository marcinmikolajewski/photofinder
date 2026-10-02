package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CloudFlareAiCallerTest {

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void callModel_givenValidResponse_whenInvoked_thenMapsDescriptionAndHeaderTokens_andConvertsImageBytes() throws Exception {
        // given
        CloudFlareAiCaller caller = new CloudFlareAiCaller(restClient);
        // inject userId for URI composition
        Field f = CloudFlareAiCaller.class.getDeclaredField("userId");
        f.setAccessible(true);
        f.set(caller, "user-123");
        Field modelField = CloudFlareAiCaller.class.getDeclaredField("MODEL");
        modelField.setAccessible(true);
        modelField.set(caller, "@cf/llava-hf/llava-1.5-7b-hf");

        CloudFlareAiCaller.AiResponse body = new CloudFlareAiCaller.AiResponse();
        CloudFlareAiCaller.AiResponse.Result result = new CloudFlareAiCaller.AiResponse.Result();
        result.setDescription("desc");
        body.setResult(result);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(555);

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        when(restClient.post()
                .uri(anyString())
                .contentType(any(MediaType.class))
                .body(bodyCaptor.capture())
                .retrieve()
                .toEntity(eq(CloudFlareAiCaller.AiResponse.class)))
            .thenReturn(ResponseEntity.ok().headers(headers).body(body));

        byte[] image = new byte[]{0, -1, 127}; // expect [0,255,127] after conversion

        // when
        ResponseAiDto response = caller.callModel(image, "prompt");

        // then
        assertThat(response.response()).isEqualTo("desc");
        assertThat(response.totalToken()).isEqualTo(555L); // from headers
        assertThat(response.status()).isEqualTo(Status.DESCRIBED);
        assertThat(response.model()).contains("llava");

        Object sentBody = bodyCaptor.getValue();
        assertThat(sentBody).isInstanceOf(CloudFlareAiCaller.AiRequest.class);
        CloudFlareAiCaller.AiRequest req = (CloudFlareAiCaller.AiRequest) sentBody;
        assertThat(req.getImage()).containsExactly(0, 255, 127);
        assertThat(req.getPrompt()).isEqualTo("prompt");
        assertThat(req.getMax_tokens()).isEqualTo(512);
    }

    @Test
    void recover_givenServerError5xx_whenRetriesExhausted_thenReturnsSoftError() throws Exception {
        // given — 500 z serwisu po wyczerpaniu retry trafia do @Recover
        CloudFlareAiCaller caller = new CloudFlareAiCaller(restClient);
        setModel(caller, "@cf/llava-hf/llava-1.5-7b-hf");

        // when
        ResponseAiDto response = caller.recover(
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR), new byte[]{1}, "prompt");

        // then — miękki ERROR, bez wyjątku (nie idzie do DLQ)
        assertThat(response.status()).isEqualTo(Status.ERROR);
        assertThat(response.response()).isEmpty();
        assertThat(response.totalToken()).isEqualTo(0L);
        assertThat(response.model()).contains("llava");
    }

    @Test
    void recover_givenTooManyRequests429_thenReturnsSoftError() throws Exception {
        // given
        CloudFlareAiCaller caller = new CloudFlareAiCaller(restClient);
        setModel(caller, "@cf/llava-hf/llava-1.5-7b-hf");

        // when
        ResponseAiDto response = caller.recover(
                HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "429",
                        HttpHeaders.EMPTY, new byte[0], null), new byte[]{1}, "prompt");

        // then
        assertThat(response.status()).isEqualTo(Status.ERROR);
        assertThat(response.response()).isEmpty();
    }

    private static void setModel(CloudFlareAiCaller caller, String model) throws Exception {
        Field modelField = CloudFlareAiCaller.class.getDeclaredField("MODEL");
        modelField.setAccessible(true);
        modelField.set(caller, model);
    }
}
