package eu.mm.software.photofinder.photosattribute.application.query;

import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;

import java.util.List;
import java.util.Map;

public interface PhotosAttributesQuery {

    List<PhotosAttributesDto> findByUser_Id();

    List<String> getDuplicatesPhotoAttribute(String userId);
    Map<String,Long> count();
    Long countBy(Status status);

    String describePhoto(String filePath, AiProvider provider);

    List<PhotosAttributesDto> findAllByProvider(String provider);
    List<PhotosAttributesDto> findAllByStatus(Status status);

}
