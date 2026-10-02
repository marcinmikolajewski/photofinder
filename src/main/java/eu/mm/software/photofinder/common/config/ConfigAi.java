package eu.mm.software.photofinder.common.config;

import com.cohere.api.Cohere;
import com.knuddels.jtokkit.api.EncodingType;
import io.qdrant.client.QdrantClient;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
@Getter
public class ConfigAi {

    public static final String VECTORDB_COLLECTION_NAME = "Photos";
    public static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";

    @Value("${groq.api.url}")
    private String groqApiUrl;
    @Value("${mistral.base-url}")
    private String mistralApiUrl;
    @Value("${hyperbolic.base-url}")
    private String hyperbolicApiUrl;
    @Value("${together.api-url}")
    private String togetherApiUrl;
    @Value("${nvidia.api.url}")
    private String nvidiaApiUrl;
    @Value("${google.api.url}")
    private String googleApiUrl;
    @Value("${cerebras.api.url}")
    private String cerebrasApiUrl;
    @Value("${cohere.api.url}")
    private String cohereApiUrl;
    @Value("${open.router.api.url}")
    private String openRouterApiUrl;
    @Value("${cloud.flare.api.url}")
    private String cloudFlareApiUrl;
    @Value("${anthropic.api.url}")
    private String anthropicApiUrl;

    @Value("${NVIDIA_API_KEY}")
    private String nvidiaApiKey;
    @Value("${OPENAI_API_KEY}")
    private String openaiaApiKey;
    @Value("${GROQ_API_KEY}")
    private String groqApiKey;
    @Value("${MISTRAL_API_KEY}")
    private String mistralApiKey;
    @Value("${TOGETHER_API_KEY}")
    private String togetherApiKey;
    @Value("${HYPERBOLIC_API_KEY}")
    private String hyperbolicApiKey;
    @Value("${GOOGLE_API_KEY}")
    private String googleApiKey;
    @Value("${CEREBRAS_API_KEY}")
    private String cerebrasApiKey;
    @Value("${COHERE_API_KEY}")
    private String cohereApiKey;
    @Value("${OPEN_ROUTER_API_KEY}")
    private String openRouterApiKey;
    @Value("${CLOUD_FLARE_API_KEY}")
    private String cloudFlareApiKey;
    @Value("${ANTHROPIC_API_KEY}")
    private String anthropicApiKey;

    @Bean(name = "mistral")
    public RestClient mistralRestClient() {
        return RestClient.builder()
                .baseUrl(mistralApiUrl)
                .defaultHeader(AUTHORIZATION, BEARER + StringUtils.SPACE + mistralApiKey)
                .build();
    }

    @Bean(name = "groq")
    public RestClient groqRestClient() {
        return RestClient.builder()
                .baseUrl(groqApiUrl)
                .defaultHeader(AUTHORIZATION, BEARER + StringUtils.SPACE + groqApiKey)
                .build();
    }

    @Bean(name = "together")
    public RestClient togetherRestClient() {
        return RestClient.builder()
                .baseUrl(togetherApiUrl)
                .defaultHeader(AUTHORIZATION, BEARER + StringUtils.SPACE + togetherApiKey)
                .build();
    }

    @Bean(name = "nvidia")
    public RestClient nvidiaRestClient() {
        return RestClient.builder()
                .baseUrl(nvidiaApiUrl)
                .defaultHeader(AUTHORIZATION, BEARER + StringUtils.SPACE + nvidiaApiKey)
                .build();
    }

    @Bean(name = "cerebras")
    public RestClient cerebrasRestClient() {
        return RestClient.builder()
                .baseUrl(cerebrasApiUrl)
                .defaultHeader(AUTHORIZATION, BEARER + StringUtils.SPACE + cerebrasApiKey)
                .build();
    }

    @Bean
    public AnthropicChatModel anthropicChatModel() {
        return AnthropicChatModel.builder()
                .anthropicApi(AnthropicApi.builder()
                        .baseUrl(anthropicApiUrl)
                        .apiKey(anthropicApiKey)
                        .build())
                .build();
    }

    @Bean(name = "openai")
    public OpenAiChatModel openAiChatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(openaiaApiKey)
                .build();

        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(openAiChatOptions)
                .build();
    }

    @Bean(name = "hyperbolic")
    public RestClient hyperbolicRestClient() {
        return RestClient.builder()
                .baseUrl(hyperbolicApiUrl)
                .defaultHeader(AUTHORIZATION, "bearer" + StringUtils.SPACE + hyperbolicApiKey)
                .build();
    }

    @Bean(name = "gemini")
    public RestClient geminiRestClient() {
        return RestClient.builder()
                .baseUrl(googleApiUrl)
                .defaultHeader("X-goog-api-key", googleApiKey)
                .build();
    }

    @Bean(name = "cohere")
    public Cohere cohereClient() {
        return Cohere.builder()
                .token(cohereApiKey)
                .clientName("photofinder")
                .build();
    }

    @Bean(name = "openRouter")
    public RestClient openRouterRestClient() {
        return RestClient.builder()
                .baseUrl(openRouterApiUrl)
                .defaultHeader(AUTHORIZATION, BEARER + StringUtils.SPACE + openRouterApiKey)
                .build();
    }

    @Bean(name = "cloudFlare")
    public RestClient cloudFlareRestClient() {
        return RestClient.builder()
                .baseUrl(cloudFlareApiUrl)
                .defaultHeader(AUTHORIZATION, BEARER + StringUtils.SPACE + cloudFlareApiKey)
                .build();
    }

    @Bean
    @Primary
    public VectorStore QdrantVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(VECTORDB_COLLECTION_NAME)
                .initializeSchema(true)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

    @Bean
    @Primary
    public BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,  // Specify the encoding type
                8000,                      // Set the maximum input token count
                0.9                        // Set the threshold factor
        );
    }
}