package eu.mm.software.photofinder.photosattribute.domain;

public interface TemporaryImageStorage {

    String save(String userId, String photoId, byte[] imageData);

    byte[] load(String imageRef);

    void delete(String imageRef);

    void deleteAllForUser(String userId);
}
