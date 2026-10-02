package eu.mm.software.photofinder.photoparams.infrastructure;

import eu.mm.software.photofinder.common.metrics.PhotoMetrics;
import eu.mm.software.photofinder.photoparams.domain.DescribeAttributes;
import eu.mm.software.photofinder.photoparams.application.query.FileQuery;
import eu.mm.software.photofinder.photoparams.domain.FileRepository;
import eu.mm.software.photofinder.photosattribute.domain.*;
import eu.mm.software.photofinder.photosattribute.domain.DuplicatePhotoException;
import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoJobMessage;
import eu.mm.software.photofinder.photosattribute.infrastructure.rabbit.UserQueueTracker;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import eu.mm.software.photofinder.common.ExifData;
import eu.mm.software.photofinder.common.ExifExtractor;

import static eu.mm.software.photofinder.common.FileUtils.imageToByte;

@Slf4j
@Service
@RequiredArgsConstructor
class FileRepositoryImpl implements FileRepository {

    private final PhotoAttributeRepository photoAttributeRepository;
    private final TemporaryImageStorage temporaryImageStorage;
    private final DomainEventPublisher eventPublisher;
    private final UserQuery userQuery;
    private final UserQueueTracker userQueueTracker;
    private final FileQuery fileQuery;
    private final PhotoMetrics photoMetrics;

    @Async
    @Override
    public void sendToDescribeFromLocalDisk(DescribeAttributes describeAttributes) {
        String loggedUserId = Optional.ofNullable(userQuery.findLoggedUserId())
                .orElseThrow(UserNotFoundException::new);

        Set<String> alreadySaved = fileQuery.findAllByLoggedUser();

        Set<String> allFiles = fileQuery.listAllFilesFromPath(
                describeAttributes.getPath(),
                describeAttributes.getExtensions());

        Set<String> newFiles = allFiles.parallelStream()
                .filter(it -> isValidFile(new File(it)))
                .filter(it -> !alreadySaved.contains(it))
                .collect(Collectors.toSet());

        log.info("Sending {} new files to {} queue", newFiles.size(), describeAttributes.getProvider());

        newFiles.parallelStream()
                .forEach(it -> processFile(it, describeAttributes.getProvider(), loggedUserId));
    }

    private void processFile(String pathToFile, String provider, String userId) {
        File file = new File(pathToFile);
        try {
            String resolvedProvider = getAiProvider(provider);
            AiProvider aiProvider = AiProvider.fromQueueName(resolvedProvider);
            ExifData exif = ExifExtractor.extract(file);
            byte[] image = imageToByte(file, aiProvider.getMaxImageDimension());
            if (image == null || image.length == 0) {
                log.error("Skipped: {} zero length", file.getAbsolutePath());
                return;
            }

            PhotoParamsDto draft = createDraft(resolvedProvider, file, userId, exif);
            String photoId = photoAttributeRepository.save(draft);

            String imageRef = temporaryImageStorage.save(userId, photoId, image);

            String exifContext = exif != null ? exif.toPromptString() : null;
            eventPublisher.publish(new PhotoJobMessage(
                    photoId,
                    userId,
                    imageRef,
                    resolvedProvider,
                    userQueueTracker.nextPriority(userId),
                    exifContext
            ));
            photoMetrics.photoQueued();

            log.info("Queued photo: {} for provider: {}", file.getName(), provider);

        } catch (DuplicatePhotoException e) {
            log.warn("Skipped duplicate: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Skipped: {} error: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    private PhotoParamsDto createDraft(String provider, File file, String userId, ExifData exif) {
        PhotoParamsDto dto = new PhotoParamsDto();
        dto.setStatus(Status.SEND_TO_QUEUE);
        dto.setPhotoDescription(StringUtils.EMPTY);
        dto.setProvider(provider);
        dto.setFileName(file.getName());
        dto.setPath(Path.of(file.getPath()).getParent().toString());
        dto.setUserId(userId);
        dto.setStorage(Storage.LOCAL.name());
        if (exif != null) {
            dto.setCamera(exif.camera());
            dto.setLens(exif.lens());
            dto.setFocalLength(exif.focalLength());
            dto.setAperture(exif.aperture());
            dto.setShutterSpeed(exif.shutterSpeed());
            dto.setIso(exif.iso());
        }
        return dto;
    }

    private boolean isValidFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            return img != null && img.getWidth() > 100 && img.getHeight() > 100;
        } catch (IOException e) {
            return false;
        }
    }

    private String getAiProvider(String aiProvider) {
        if (aiProvider.equalsIgnoreCase(AiProvider.RANDOM.name())) {
            List<AiProvider> available = Arrays.stream(AiProvider.values())
                    .filter(it -> !it.isPaid())
                    .filter(AiProvider::isActive)
                    .toList();
            return available.get(new Random().nextInt(available.size())).name();
        }
        return aiProvider;
    }
}