package eu.mm.software.photofinder.photosattribute.application.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditSummaryDto {

    private String provider;
    private Long totalPhotos;
    private Long totalTokens;
    private Double avgProcessingMs;
}
