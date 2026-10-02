package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CerebrasAiCaller extends OpenAiAbstractCaller {

    @Value("${cerebras.model.name:gpt-oss-120b}")
    private String MODEL;
    private final String URI = "/v1/chat/completions";


    public CerebrasAiCaller(@Qualifier("cerebras") RestClient restClient) {
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
