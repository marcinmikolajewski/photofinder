package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;


public abstract class AbstractAiService {

    public static final double TEMPERATURE = 0.3;

    public abstract AiProvider getProvider();

    protected abstract ResponseAiDto callModel(byte[] imageData, String prompt);

    public ResponseAiDto describePhoto(byte[] imageData) {
        String prompt = "Describe this image in english";

        return callModel(imageData, prompt);
    }
}
