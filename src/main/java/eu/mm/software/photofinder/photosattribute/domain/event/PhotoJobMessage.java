package eu.mm.software.photofinder.photosattribute.domain.event;

public record PhotoJobMessage(
        String photoId,
        String userId,
        String imageRef,
        String provider,
        int priority,
        String exifContext
) implements DomainEvent {
}
