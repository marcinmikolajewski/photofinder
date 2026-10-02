package eu.mm.software.photofinder.photosattribute.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhotosApplicationService {

    private final PhotoRepository photoRepository;

    public void deleteDuplicates(String userId) {
        photoRepository.deleteDuplicates(userId);
    }

    public void deleteDescribePhotosFromDBWhenWasDeleteFromDisk() {
        photoRepository.deleteDescribePhotosFromDBWhenWasDeleteFromDisk();
    }

    public void deletePhotosFromDBWhenStatusISError() {
        photoRepository.deletePhotosFromDBWhenStatusISError();
    }
}
