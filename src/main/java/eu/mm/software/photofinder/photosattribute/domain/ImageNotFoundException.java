package eu.mm.software.photofinder.photosattribute.domain;

public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(String imageRef) {
        super("Temporary image not found in storage: " + imageRef);
    }
}
