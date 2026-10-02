package eu.mm.software.photofinder.photosattribute.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "photos_202606")
@CompoundIndex(def = "{'path': 1, 'filename': 1, 'userId': 1, 'storage': 1}", unique = true)
public class PhotoAttribute {

    @Id
    private String id;
    @Indexed
    private String filename;
    @Indexed
    private String path;
    private String photoDescription;
    @Indexed
    private String provider;
    @Indexed
    private String model;
    @Indexed
    private Status status;
    @Indexed
    private String userId;
    @Indexed
    private Storage storage;

    @Indexed
    private String camera;
    @Indexed
    private String lens;
    private String focalLength;
    private String aperture;
    private String shutterSpeed;
    @Indexed
    private Integer iso;
}
