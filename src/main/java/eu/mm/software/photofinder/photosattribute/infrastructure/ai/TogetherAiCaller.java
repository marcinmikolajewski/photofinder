package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class TogetherAiCaller extends OpenAiAbstractCaller {

    @Value("${together.model.name:google/gemma-3n-E4B-it}")
    private String MODEL;
    private final static String URI = "v1/chat/completions";

    @Autowired
    public TogetherAiCaller(@Qualifier("together") RestClient restClient) {
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
