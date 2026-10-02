package eu.mm.software.photofinder.photoparams.application.command;

import eu.mm.software.photofinder.photoparams.domain.DescribeAttributes;

import eu.mm.software.photofinder.photoparams.domain.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileApplicationService {

    private final FileRepository fileRepository;

    @Async
    public void describePhotos(DescribeAttributes describeAttributes) {

        fileRepository.sendToDescribeFromLocalDisk(describeAttributes);
    }
}