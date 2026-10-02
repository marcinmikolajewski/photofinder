package eu.mm.software.photofinder.photosattribute.domain;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoParamsDto implements Serializable {

    private String id;
    private String fileName;
    private String path;
    private String photoDescription;
    private String provider;
    private String model;
    private Status status;
    private byte[] image;
    private String userId;
    private String storage;

    private String camera;
    private String lens;
    private String focalLength;
    private String aperture;
    private String shutterSpeed;
    private Integer iso;
}
