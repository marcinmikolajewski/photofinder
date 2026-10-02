package eu.mm.software.photofinder.photosattribute.application.query;

import eu.mm.software.photofinder.photosattribute.domain.AiProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuditPhotosQuery {

    Set<AuditPhotoDto> findByLoggedUser();

    Set<AuditPhotoDto> findByUserEmailAndProvider(AiProvider provider);

    Map<String,AuditPhotoStatsDto> getStats();

    Set<String> findIdPhotosWithEmptyDescription();

    List<AuditPhotoDto> findByUserId(String userId);

    List<AuditSummaryDto> summarizeByProvider(String userId);


}
