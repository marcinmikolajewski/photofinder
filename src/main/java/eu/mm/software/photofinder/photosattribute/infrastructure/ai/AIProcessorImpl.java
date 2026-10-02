package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import eu.mm.software.photofinder.photosattribute.application.query.AIProcessor;
import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
class AIProcessorImpl implements AIProcessor {

    private final Map<AiProvider, AbstractAiService> aiServices;


    AIProcessorImpl(List<AbstractAiService> allAiServices) {
        aiServices = new EnumMap<>(AiProvider.class);
        for (AbstractAiService service : allAiServices) {
            AiProvider provider = service.getProvider();
            if (provider != null && provider != AiProvider.RANDOM) {
                aiServices.put(provider, service);
                log.info("Registered AI service: {} -> {}", provider, service.getClass().getSimpleName());
            }
        }

        for (AiProvider provider : AiProvider.values()) {
            if (provider != AiProvider.RANDOM && provider.isActive() && !aiServices.containsKey(provider)) {
                log.warn("No AI service registered for active provider: {}", provider);
            }
        }
    }

    @Override
    public ResponseAiDto describePhoto(byte[] imageData, AiProvider provider) {
        Assert.notNull(imageData, "imageData must not be null");
        Assert.notNull(provider, "provider must not be null");

        AiProvider aiProvider = resolveProvider(provider);
        AbstractAiService aiService = Optional.ofNullable(aiServices.get(aiProvider))
                .orElseThrow(() -> new IllegalStateException(
                        "No AI service available for provider: " + aiProvider));
        log.info("Get image description from: {}", aiProvider);

        return aiService.describePhoto(imageData);
    }

    private AiProvider resolveProvider(AiProvider aiProvider) {

        if (aiProvider == AiProvider.RANDOM) {
            List<AiProvider> availableProviders = Arrays.stream(AiProvider.values())
                    .filter(it -> it != AiProvider.RANDOM)
                    .filter(it -> !it.isPaid())
                    .filter(AiProvider::isActive)
                    .filter(aiServices::containsKey)  // FIX: tylko te, które mają zarejestrowany serwis
                    .toList();

            if (availableProviders.isEmpty()) {
                throw new IllegalStateException("No active free AI providers available for RANDOM selection");
            }

            return availableProviders.get(ThreadLocalRandom.current().nextInt(availableProviders.size()));
        }
        return aiProvider;
    }
}
