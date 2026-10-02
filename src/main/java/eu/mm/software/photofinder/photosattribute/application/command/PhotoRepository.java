package eu.mm.software.photofinder.photosattribute.application.command;

public interface PhotoRepository {

    void deleteDuplicates(String userId);

    void deleteDescribePhotosFromDBWhenWasDeleteFromDisk();

    void deletePhotosFromDBWhenStatusISError();
}
