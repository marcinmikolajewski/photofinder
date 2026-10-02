package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
class CerebrasAiService extends AbstractAiService {

    private final CerebrasAiCaller cerebrasAiCaller;

    @Override
    public AiProvider getProvider() {
        return AiProvider.CEREBRAS;
    }

    @Override
    protected ResponseAiDto callModel(byte[] imageData, String prompt) {
        return cerebrasAiCaller.callModel(imageData, prompt);
    }
}
