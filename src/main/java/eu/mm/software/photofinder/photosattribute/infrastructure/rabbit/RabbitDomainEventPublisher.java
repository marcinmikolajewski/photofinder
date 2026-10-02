package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.event.*;
import eu.mm.software.photofinder.user.domain.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final Map<Class<? extends DomainEvent>, String> ROUTING_KEYS = Map.of(
            PhotoJobMessage.class, "ai.event.photo.job",
            PhotoProcessedEvent.class, "ai.event.photo.processed",
            PhotoDescribedEvent.class, "ai.event.photo.described",
            PhotoAuditedEvent.class,   "app.event.photo.audited",
            UserDeletedEvent.class, "app.user.cleanup.queue"
    );

    @Override
    public void publish(DomainEvent event) {

        if (event instanceof PhotoJobMessage jobMessage) {
            // routing bezpośrednio do kolejki providera np. "ai.describe.ollama"
            String routingKey = "ai.describe." + jobMessage.provider().toLowerCase();

            log.info("Publishing PhotoJobMessage to routing key: {}", routingKey);

            rabbitTemplate.convertAndSend("ai.exchange", routingKey, jobMessage, message -> {
                message.getMessageProperties().setPriority(jobMessage.priority());
                return message;
            });
            return;
        }

        String routingKey = ROUTING_KEYS.get(event.getClass());
        if (routingKey == null) {
            throw new IllegalArgumentException(
                    "No routing key defined for event: " + event.getClass().getSimpleName());
        }

        log.info("Publishing event: {} with routing key: {}",
                event.getClass().getSimpleName(), routingKey);

        rabbitTemplate.convertAndSend("ai.exchange", routingKey, event);
    }
}
