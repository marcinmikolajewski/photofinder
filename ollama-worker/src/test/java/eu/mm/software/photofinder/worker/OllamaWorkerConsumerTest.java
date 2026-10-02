package eu.mm.software.photofinder.worker;

import eu.mm.software.photofinder.worker.event.PhotoJobMessage;
import eu.mm.software.photofinder.worker.storage.WorkerMinioStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OllamaWorkerConsumerTest {

    private OllamaChatModel chatModel;
    private WorkerMinioStorage minioStorage;
    private RabbitTemplate rabbitTemplate;
    private WorkerNodeInfo workerNodeInfo;

    private OllamaWorkerConsumer consumer;

    @BeforeEach
    void setUp() {
        chatModel = mock(OllamaChatModel.class);
        minioStorage = mock(WorkerMinioStorage.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        workerNodeInfo = mock(WorkerNodeInfo.class);
        when(workerNodeInfo.getNodeLabel()).thenReturn("test-node");

        consumer = new OllamaWorkerConsumer(chatModel, minioStorage, rabbitTemplate, workerNodeInfo);
    }

    @Test
    void receive_propagatesException_andDoesNotDeleteImage_whenModelFails() {
        // given — obraz załadowany, ale wnioskowanie Ollamy rzuca błąd
        PhotoJobMessage message = new PhotoJobMessage(
                "photo-1", "user-1", "photos/tmp/user-1/photo-1.jpg", "OLLAMA", 0, null);
        when(minioStorage.load(message.imageRef())).thenReturn(new byte[]{1, 2, 3});
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Ollama down"));

        // when / then — wyjątek MUSI propagować, żeby factory zrobiło retry → DLQ
        assertThatThrownBy(() -> consumer.receive(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ollama down");

        // KLUCZOWE: obraz NIE może zostać skasowany przed sukcesem (inaczej retry/DLQ traci dane)
        verify(minioStorage, never()).delete(anyString());
        // żaden event o przetworzeniu nie powinien zostać wysłany
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void receive_propagatesException_andDoesNotDeleteImage_whenImageLoadFails() {
        // given — sam load obrazu pada (np. MinIO niedostępne)
        PhotoJobMessage message = new PhotoJobMessage(
                "photo-2", "user-1", "photos/tmp/user-1/photo-2.jpg", "OLLAMA", 0, null);
        when(minioStorage.load(message.imageRef()))
                .thenThrow(new RuntimeException("MinIO unavailable"));

        // when / then
        assertThatThrownBy(() -> consumer.receive(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MinIO unavailable");

        verify(minioStorage, never()).delete(anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verifyNoInteractions(chatModel);
    }
}
