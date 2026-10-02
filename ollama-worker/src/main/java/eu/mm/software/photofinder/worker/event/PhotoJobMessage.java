package eu.mm.software.photofinder.worker.event;

public record PhotoJobMessage(
        String photoId,
        String userId,
        String imageRef,
        String provider,
        int priority,
        String exifContext
) {
}
