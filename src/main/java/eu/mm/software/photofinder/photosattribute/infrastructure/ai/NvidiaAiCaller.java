package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class NvidiaAiCaller extends OpenAiAbstractCaller {

    @Value("${nvidia.model.name:google/gemma-3-27b-it}")
    private String MODEL;
    private final static String URI = "v1/chat/completions";

    public NvidiaAiCaller(@Qualifier("nvidia") RestClient nvidiaRestClient) {
        super(nvidiaRestClient);
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
