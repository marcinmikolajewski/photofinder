package eu.mm.software.photofinder.photosattribute.application.command;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditPhotosApplicationService {

    private final AuditPhotosRepository auditPhotosRepository;

    public void removeUnlinkAuditPhotos() {
        auditPhotosRepository.removeUnlinkAuditPhotos();
    }
}
