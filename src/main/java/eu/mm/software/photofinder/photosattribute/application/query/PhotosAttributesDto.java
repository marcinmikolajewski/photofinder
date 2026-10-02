package eu.mm.software.photofinder.photosattribute.application.query;

import eu.mm.software.photofinder.photosattribute.domain.Status;

public record PhotosAttributesDto(String id,
                                  String filename,
                                  String path,
                                  String photoDescription,
                                  String provider,
                                  String model,
                                  Status status,
                                  String userName,
                                  String storage) {

}
