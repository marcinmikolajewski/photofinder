package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddedConsumer {

    private final EmbeddedProcessingService embeddedProcessingService;

    @RabbitListener(queues = "ai.embedded.queue", containerFactory = "rabbitMultiCoreListenerContainerFactory")
    public void receiveFromEmbedded(List<DocumentMessage> message) throws JsonProcessingException {

        embeddedProcessingService.processMessage(message);
    }
}
