package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.mm.software.photofinder.common.WorkerNodeInfo;
import eu.mm.software.photofinder.common.metrics.PhotoMetrics;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttributeRepository;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoAuditedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DeadLetterConsumerTest {

    private PhotoAttributeRepository photoAttributeRepository;
    private DomainEventPublisher eventPublisher;
    private WorkerNodeInfo workerNodeInfo;
    private PhotoMetrics photoMetrics;
    private DeadLetterConsumer consumer;

    @BeforeEach
    void setUp() {
        photoAttributeRepository = mock(PhotoAttributeRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        workerNodeInfo = mock(WorkerNodeInfo.class);
        photoMetrics = mock(PhotoMetrics.class);
        when(workerNodeInfo.getNodeLabel()).thenReturn("test-node");

        consumer = new DeadLetterConsumer(
                photoAttributeRepository, eventPublisher, workerNodeInfo, new ObjectMapper(), photoMetrics);
    }

    @Test
    void onDeadLetter_updatesStatusPublishesAuditAndCountsErrorOnce_whenMessageFromAiProviderQueue() {
        // given
        String body = """
                {"photoId":"photo-1","userId":"user-1","imageRef":"ref","provider":"GROQ","priority":5,"exifContext":null}
                """;
        Message message = buildMessage(body, "ai.groq.queue");

        // when
        consumer.onDeadLetter(message);

        // then
        verify(photoAttributeRepository).updateStatusToError("photo-1");
        // metryka błędu liczona DOKŁADNIE RAZ, finalnie w DLQ
        verify(photoMetrics, times(1)).photoDescribeError("GROQ");

        ArgumentCaptor<PhotoAuditedEvent> captor = ArgumentCaptor.forClass(PhotoAuditedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        PhotoAuditedEvent event = captor.getValue();
        assertThat(event.photoId()).isEqualTo("photo-1");
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.provider()).isEqualTo("GROQ");
        assertThat(event.status()).isEqualTo(Status.ERROR.name());
        assertThat(event.workerNode()).isEqualTo("test-node");
    }

    @Test
    void onDeadLetter_doesNothing_whenMessageFromNonAiQueue() {
        // given
        Message message = buildMessage("""
                {"photoId":"photo-2","userId":"user-1"}
                """, "app.photo.save.queue");

        // when
        consumer.onDeadLetter(message);

        // then
        verifyNoInteractions(photoAttributeRepository, eventPublisher, photoMetrics);
    }

    @Test
    void onDeadLetter_ignoresEmbeddedQueue_evenThoughNameLooksLikeAiQueue() {
        // given — "ai.embedded.queue" pasuje do wzorca "ai.*.queue", ale NIE przewozi PhotoJobMessage
        Message message = buildMessage("{\"some\":\"embedded-doc\"}", "ai.embedded.queue");

        // when
        consumer.onDeadLetter(message);

        // then — segment "embedded" nie jest providerem z AiProvider → pomijamy
        verifyNoInteractions(photoAttributeRepository, eventPublisher, photoMetrics);
    }

    @Test
    void onDeadLetter_ignoresUserDeletedQueue_evenThoughNameLooksLikeAiQueue() {
        // given — "ai.user.deleted.queue" przewozi UserDeletedEvent, nie PhotoJobMessage
        Message message = buildMessage("{\"userId\":\"user-9\"}", "ai.user.deleted.queue");

        // when
        consumer.onDeadLetter(message);

        // then
        verifyNoInteractions(photoAttributeRepository, eventPublisher, photoMetrics);
    }

    @Test
    void onDeadLetter_logsWarningAndDoesNotCrash_whenBodyIsMalformed() {
        // given — kolejka jest providera (ollama), ale body nie jest poprawnym JSON-em
        Message message = buildMessage("not-json-at-all", "ai.ollama.queue");

        // when / then — nie powinno rzucić wyjątku ani nic zmienić
        consumer.onDeadLetter(message);

        verifyNoInteractions(photoAttributeRepository, eventPublisher, photoMetrics);
    }

    private Message buildMessage(String body, String originalQueue) {
        MessageProperties props = new MessageProperties();
        props.setHeader("x-first-death-queue", originalQueue);
        props.setHeader("x-first-death-reason", "rejected");
        return new Message(body.getBytes(), props);
    }
}
