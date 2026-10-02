package eu.mm.software.photofinder.photosattribute.domain.event;

public record PhotoAuditedEvent(
        String photoId,
        String userId,
        String provider,
        String model,
        Long processingTimeMs,
        Long totalTokens,
        String status,
        String workerNode
) implements DomainEvent {
}