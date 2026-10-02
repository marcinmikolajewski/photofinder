package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class MistralAiService extends AbstractAiService {

    private final MistralAiCaller mistralAiCaller;

    @Override
    public AiProvider getProvider() {
        return AiProvider.MISTRAL;
    }

    @Override
    protected ResponseAiDto callModel(byte[] imageData, String prompt) {
        return mistralAiCaller.callModel(imageData, prompt);
    }
}