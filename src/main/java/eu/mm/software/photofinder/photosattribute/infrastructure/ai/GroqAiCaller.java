package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GroqAiCaller extends OpenAiAbstractCaller {

    private final static String URI = "/v1/chat/completions";

    @Value("${groq.model.name:meta-llama/llama-4-scout-17b-16e-instruct}")
    private String MODEL;

    public GroqAiCaller(@Qualifier("groq") RestClient restClient) {
        super(restClient);
    }

    @Override
    protected String getModel() {
        return MODEL;
    }

    @Override
    protected String getUri() {
        return URI;
    }
}
