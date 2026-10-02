package eu.mm.software.photofinder.photosattribute.domain;

public class DuplicatePhotoException extends RuntimeException {
    public DuplicatePhotoException(String path, String filename) {
        super("Duplicate photo skipped: " + path + "/" + filename);
    }
}
