package eu.mm.software.photofinder.photosattribute.application.query;

import java.time.Duration;

public record AuditPhotoStatsDto(int numberPhotos,
                                 long tokens,
                                 Duration timestamp,
                                 double avgToken,
                                 Duration anvTimestamp) {
}
