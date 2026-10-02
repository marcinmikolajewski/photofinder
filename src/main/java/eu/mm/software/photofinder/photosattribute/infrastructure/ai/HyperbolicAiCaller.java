package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class HyperbolicAiCaller extends OpenAiAbstractCaller {

    @Value("${hyperbolic.model.name:deepseek-ai/DeepSeek-V3-0324}")
    private String MODEL;
    private final static String URI = "v1/chat/completions";

    public HyperbolicAiCaller(@Qualifier("hyperbolic") RestClient restClient) {
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
