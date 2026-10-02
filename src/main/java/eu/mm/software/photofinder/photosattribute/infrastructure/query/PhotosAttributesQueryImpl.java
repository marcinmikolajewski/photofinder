package eu.mm.software.photofinder.photosattribute.infrastructure.query;

import eu.mm.software.photofinder.common.security.PathAccessGuard;
import eu.mm.software.photofinder.photosattribute.application.query.AIProcessor;
import eu.mm.software.photofinder.photosattribute.application.query.PhotosAttributesDto;
import eu.mm.software.photofinder.photosattribute.application.query.PhotosAttributesQuery;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.PhotoAttributeSpringDataRepository;
import eu.mm.software.photofinder.photosattribute.infrastructure.repository.UserCacheService;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static eu.mm.software.photofinder.common.FileUtils.imageToByte;

@Service
@RequiredArgsConstructor
@Slf4j
class PhotosAttributesQueryImpl implements PhotosAttributesQuery {

    private final PhotoAttributeSpringDataRepository repository;
    private final AIProcessor aiProcessor;
    private final UserQuery userQuery;
    private final UserCacheService userCacheService;
    private final PathAccessGuard pathAccessGuard;

    @Override
    public List<PhotosAttributesDto> findByUser_Id() {

        String loggedUserId = userQuery.findLoggedUserId();

        return repository.findByUserId(loggedUserId).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public Map<String, Long> count() {
        String loggedUserId = userQuery.findLoggedUserId();

        return Arrays.stream(Status.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        s -> repository.countAllByStatusAndUserId(s, loggedUserId)));
    }

    @Override
    public Long countBy(Status status) {
        String loggedUserId = userQuery.findLoggedUserId();
        return repository.countAllByStatusAndUserId(status, loggedUserId);
    }

    @Override
    public List<String> getDuplicatesPhotoAttribute(String userId) {

        return repository.findDuplicatePhotoIds(userId).parallelStream()
                .map(ObjectId::toString)
                .toList();
    }

    @Override
    public String describePhoto(String filePath, AiProvider provider) {

        File validatedFile = pathAccessGuard.resolveWithinAllowedRoots(filePath).toFile();

        return aiProcessor.describePhoto(imageToByte(validatedFile, provider.getMaxImageDimension()), provider)
                .response();
    }

    @Override
    public List<PhotosAttributesDto> findAllByProvider(String provider) {

        return repository.findAllByProviderAndUserId(provider,
                        userQuery.findLoggedUserId()).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<PhotosAttributesDto> findAllByStatus(Status status) {
        return repository.findAllByStatusAndUserId(status, userQuery.findLoggedUserId()).stream()
                .map(this::convertToDto)
                .toList();
    }

    private PhotosAttributesDto convertToDto(PhotoAttribute photoAttribute) {
        return new PhotosAttributesDto(photoAttribute.getId(),
                photoAttribute.getFilename(),
                photoAttribute.getPath(),
                photoAttribute.getPhotoDescription(),
                photoAttribute.getProvider(),
                photoAttribute.getModel(),
                photoAttribute.getStatus(),
                resolveUserName(photoAttribute.getUserId()),
                Optional.of(photoAttribute)
                        .map(PhotoAttribute::getStorage)
                        .map(Enum::name)
                        .orElse("unknown"));
    }

    private String resolveUserName(String userId) {
        if (userId == null) {
            return "unknown";
        }
        try {
            return userCacheService.getUser(userId).getUserName();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
