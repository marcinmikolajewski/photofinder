package eu.mm.software.photofinder.photosattribute.domain;

public interface VectorDBRepository {

    void sendEmbeddedToQueue(Boolean overwrite);
    void sendEmbeddedToQueue(PhotoAttribute photoAttribute);
    void deleteAllByUserId(String userId);

}
