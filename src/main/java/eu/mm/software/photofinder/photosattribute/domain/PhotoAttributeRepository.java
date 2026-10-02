package eu.mm.software.photofinder.photosattribute.domain;


public interface PhotoAttributeRepository {

    String save(PhotoParamsDto form);

    void saveVector(String photoId);

    void updateAfterDescribe(String photoId,
                             String description,
                             String model,
                             Status status,
                             String provider);

    void updateStatusToError(String photoId);

    void deleteAllByUserId(String userId);
}
