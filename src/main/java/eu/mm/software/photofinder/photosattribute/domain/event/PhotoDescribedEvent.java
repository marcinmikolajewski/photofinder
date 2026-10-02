package eu.mm.software.photofinder.photosattribute.domain.event;

public record PhotoDescribedEvent(
        String photoId,
        String userId,
        String provider
) implements DomainEvent {}