package eu.mm.software.photofinder.photosattribute.domain.event;

import eu.mm.software.photofinder.photosattribute.domain.Status;

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
) implements DomainEvent {
}