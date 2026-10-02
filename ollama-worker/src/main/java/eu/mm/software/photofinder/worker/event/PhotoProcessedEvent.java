package eu.mm.software.photofinder.worker.event;

public record PhotoProcessedEvent(
        String photoId,
        String userId,
        String description,
        String model,
        Status status,
        String provider,
        Long processingTimeMs,
        Long totalTokens,
        String workerNode
) {
}
