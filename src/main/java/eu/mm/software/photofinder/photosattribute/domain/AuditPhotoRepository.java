package eu.mm.software.photofinder.photosattribute.domain;

public interface AuditPhotoRepository {

    void save(AuditPhotos audit);

    void deleteAllByUserId(String userId);

}
