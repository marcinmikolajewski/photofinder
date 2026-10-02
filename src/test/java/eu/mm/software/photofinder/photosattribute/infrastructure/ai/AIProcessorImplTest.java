package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AIProcessorImplTest {

    private AIProcessorImpl processor;

    @BeforeEach
    void setUp() {
        // Create mocks for required service dependencies
        OllamaService ollamaService = mock(OllamaService.class);
        GroqAiRestService groqService = mock(GroqAiRestService.class);
        MistralAiService mistralService = mock(MistralAiService.class);
        NvidiaAiService nvidiaService = mock(NvidiaAiService.class);
        TogetherAiRestService togetherService = mock(TogetherAiRestService.class);
        OpenAiService openAiService = mock(OpenAiService.class);
        AnthropicAiService anthropicAiService = mock(AnthropicAiService.class);
        HyperbolicAiService hyperbolicAiService = mock(HyperbolicAiService.class);
        GeminiAiService geminiAiService = mock(GeminiAiService.class);
        CerebrasAiService cerebrasAiService = mock(CerebrasAiService.class);
        CohereAiService cohereAiService = mock(CohereAiService.class);
        OpenRouterAiService openRouterAiService = mock(OpenRouterAiService.class);
        CloudFlareAiService cloudFlareAiService = mock(CloudFlareAiService.class);

        // Use the package-visible test bridge to construct the processor
        processor = new AIProcessorImplAccessible(List.of(
                ollamaService,
                groqService,
                mistralService,
                nvidiaService,
                togetherService,
                openAiService,
                anthropicAiService,
                hyperbolicAiService,
                geminiAiService,
                cerebrasAiService,
                cohereAiService,
                openRouterAiService,
                cloudFlareAiService
        ));
    }

    @Test
    void describePhoto_nullImage_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> processor.describePhoto(null, AiProvider.OLLAMA));
        assertTrue(ex.getMessage().contains("imageData must not be null"));
    }

    @Test
    void describePhoto_nullProvider_throwsIllegalArgumentException() {
        byte[] data = new byte[]{1,2,3};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> processor.describePhoto(data, null));
        assertTrue(ex.getMessage().contains("provider must not be null"));
    }

    @Test
    void describePhoto_unknownProvider_throwsIllegalStateException() throws Exception {
        // Remove one provider from the internal map to simulate unknown
        Map<AiProvider, AbstractAiService> map = getInternalMap(processor);
        map.remove(AiProvider.OLLAMA);
        byte[] data = new byte[]{42};
        assertThrows(IllegalStateException.class, () -> processor.describePhoto(data, AiProvider.OLLAMA));
    }

    @Test
    void describePhoto_delegatesToCorrectService_andReturnsResponse() throws Exception {
        // Replace a specific provider service with test stub
        Map<AiProvider, AbstractAiService> map = getInternalMap(processor);
        CapturingService stub = new CapturingService();
        map.put(AiProvider.OPENAI, stub);

        byte[] data = new byte[]{9,9,9};
        ResponseAiDto result = processor.describePhoto(data, AiProvider.OPENAI);

        assertArrayEquals(data, stub.captured);
        assertNotNull(result);
        assertEquals("ok", result.response());
        assertEquals(Status.DESCRIBED, result.status());
    }

    @Test
    void describePhoto_givenRandomProvider_whenFreeActiveAvailable_thenDelegatesToOneOfFreeActiveServices() throws Exception {
        // given
        Map<AiProvider, AbstractAiService> map = getInternalMap(processor);
        CapturingService stub = new CapturingService();
        for (AiProvider p : AiProvider.values()) {
            if (!p.isPaid() && p.isActive()) {
                map.put(p, stub);
            }
        }
        byte[] data = new byte[]{5,5};

        // when
        ResponseAiDto res = processor.describePhoto(data, AiProvider.RANDOM);

        // then
        assertArrayEquals(data, stub.captured);
        assertNotNull(res);
        assertEquals(Status.DESCRIBED, res.status());
        assertEquals("ok", res.response());
    }

    @SuppressWarnings("unchecked")
    private Map<AiProvider, AbstractAiService> getInternalMap(AIProcessorImpl impl) throws Exception {
        Field f = AIProcessorImpl.class.getDeclaredField("aiServices");
        f.setAccessible(true);
        return (Map<AiProvider, AbstractAiService>) f.get(impl);
    }

    private static class CapturingService extends AbstractAiService {
        byte[] captured;

        @Override
        public AiProvider getProvider() {
            return AiProvider.OPENAI;
        }

        @Override
        protected ResponseAiDto callModel(byte[] imageData, String prompt) {
            this.captured = imageData;
            return new ResponseAiDto("ok", 1L, "test-model", Status.DESCRIBED);
        }
    }
}
