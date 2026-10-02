package eu.mm.software.photofinder.photosattribute.application.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditPhotoDto {

    private String id;
    private String photoAttributeId;
    private String provider;
    private String model;
    private Long totalTokens;
    private Long processingTimeMs;
    private String status;
    private String workerNode;
    private Instant occurredAt;
}