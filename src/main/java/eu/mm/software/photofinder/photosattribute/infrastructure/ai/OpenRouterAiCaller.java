package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class OpenRouterAiCaller extends OpenAiAbstractCaller {

    @Value("${openrouter.model.name:google/gemini-2.0-flash-001}")
    private String MODEL;
    private final static String URI = "v1/chat/completions";

    @Autowired
    public OpenRouterAiCaller(@Qualifier("openRouter") RestClient restClient) {
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
