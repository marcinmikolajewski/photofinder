package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.common.WorkerNodeInfo;
import eu.mm.software.photofinder.common.metrics.PhotoMetrics;
import eu.mm.software.photofinder.photosattribute.application.query.AIProcessor;
import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.TemporaryImageStorage;
import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoJobMessage;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoProcessedEvent;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import eu.mm.software.photofinder.photosattribute.domain.ImageNotFoundException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PhotosConsumerTest {

    private AIProcessor aiProcessor;
    private TemporaryImageStorage temporaryImageStorage;
    private DomainEventPublisher eventPublisher;
    private PhotoMetrics photoMetrics;
    private WorkerNodeInfo workerNodeInfo;

    private PhotosConsumer consumer;

    @BeforeEach
    void setUp() {
        aiProcessor = mock(AIProcessor.class);
        temporaryImageStorage = mock(TemporaryImageStorage.class);
        eventPublisher = mock(DomainEventPublisher.class);
        photoMetrics = mock(PhotoMetrics.class);
        workerNodeInfo = mock(WorkerNodeInfo.class);
        when(workerNodeInfo.getNodeLabel()).thenReturn("PC-Test | Intel i7 x8 cores");

        consumer = new PhotosConsumer(aiProcessor, temporaryImageStorage, eventPublisher, photoMetrics, workerNodeInfo);
    }

    @Test
    void process_loadsImageDescribesAndPublishesEvent() {
        // given
        byte[] imageData = {1, 2, 3};
        PhotoJobMessage message = new PhotoJobMessage("photo-1", "user-1", "photos/tmp/user-1/photo-1.jpg", "OLLAMA", 0, null);

        ResponseAiDto aiResponse = new ResponseAiDto("a description", 123L, "some-model", Status.DESCRIBED);
        when(temporaryImageStorage.load(message.imageRef())).thenReturn(imageData);
        when(aiProcessor.describePhoto(any(), any())).thenReturn(aiResponse);

        // when
        consumer.process(message);

        // then
        verify(aiProcessor).describePhoto(eq(imageData), eq(AiProvider.OLLAMA));
        verify(temporaryImageStorage).delete(message.imageRef());

        ArgumentCaptor<PhotoProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PhotoProcessedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        PhotoProcessedEvent event = eventCaptor.getValue();
        assertThat(event.photoId()).isEqualTo("photo-1");
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.description()).isEqualTo("a description");
        assertThat(event.model()).isEqualTo("some-model");
        assertThat(event.status()).isEqualTo(Status.DESCRIBED);
        assertThat(event.provider()).isEqualTo("OLLAMA");
        assertThat(event.totalTokens()).isEqualTo(123L);
        assertThat(event.workerNode()).isEqualTo("PC-Test | Intel i7 x8 cores");
    }

    @Test
    void receive_throwsAmqpRejectAndDontRequeue_whenImageNotFound() {
        // given
        PhotoJobMessage message = new PhotoJobMessage("photo-1", "user-1", "photos/tmp/user-1/photo-1.jpg", "OLLAMA", 0, null);
        when(temporaryImageStorage.load(any())).thenThrow(new ImageNotFoundException("photos/tmp/user-1/photo-1.jpg"));

        // when / then — ImageNotFoundException musi trafić do DLQ natychmiast (bez retry)
        assertThatThrownBy(() -> consumer.receive(message))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        // metryka błędu liczona dopiero finalnie w DeadLetterConsumer, NIE per próba w receive()
        verify(photoMetrics, never()).photoDescribeError(anyString());
    }

    @Test
    void receive_rethrowsRuntimeException_whenAiProviderFails() {
        // given
        PhotoJobMessage message = new PhotoJobMessage("photo-2", "user-1", "photos/tmp/user-1/photo-2.jpg", "GROQ", 0, null);
        byte[] imageData = {1, 2, 3};
        when(temporaryImageStorage.load(any())).thenReturn(imageData);
        when(aiProcessor.describePhoto(any(), any())).thenThrow(new RuntimeException("API timeout"));

        // when / then — RuntimeException musi propagować, żeby factory zrobiło retry
        assertThatThrownBy(() -> consumer.receive(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API timeout");

        // metryka błędu liczona dopiero finalnie w DeadLetterConsumer, NIE per próba w receive()
        verify(photoMetrics, never()).photoDescribeError(anyString());
    }
}
