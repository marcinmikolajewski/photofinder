package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
class OllamaService extends AbstractAiService {

    private final OllamaAiCaller ollamaAiCaller;

    @Override
    public AiProvider getProvider() {
        return AiProvider.OLLAMA;
    }

    @Override
    protected ResponseAiDto callModel(byte[] imageData, String prompt) {
        return ollamaAiCaller.callModel(imageData, prompt);
    }
}