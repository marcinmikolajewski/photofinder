package eu.mm.software.photofinder.photosattribute.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "auditPhotos_202606")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuditPhotos {

    public static final String COLLECTION = "auditPhotos_202606";

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String photoAttributeId;

    @Indexed
    private String provider;
    @Indexed
    private String model;

    private Long totalTokens;
    private Long processingTimeMs;

    @Indexed
    private String status;

    private String workerNode;

    @Indexed
    private Instant occurredAt;
}
