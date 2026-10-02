package eu.mm.software.photofinder.photosattribute.infrastructure.repository;

import eu.mm.software.photofinder.common.FileUtils;
import eu.mm.software.photofinder.photosattribute.application.command.PhotoRepository;
import eu.mm.software.photofinder.photosattribute.domain.AuditPhotos;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.AuditPhotoSpringDataRepository;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.PhotoAttributeSpringDataRepository;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
class PhotoRepositoryImpl implements PhotoRepository {

    private final PhotoAttributeSpringDataRepository repository;
    private final UserQuery userQuery;
    private final AuditPhotoSpringDataRepository auditPhotoSpringDataRepository;

    public void deleteDuplicates(String userId) {
        List<ObjectId> duplicatePhotoIds = repository.findDuplicatePhotoIds(userId);

        log.info("duplicatePhotoIds: {}", duplicatePhotoIds.size());

        repository.deleteAllById(duplicatePhotoIds.stream().map(ObjectId::toString).toList());
    }

    @Override
    public void deleteDescribePhotosFromDBWhenWasDeleteFromDisk() {

        List<PhotoAttribute> list = repository.findByUserId(userQuery.findLoggedUserId()).parallelStream()
                .filter(FileUtils::fileNotExist)
                .toList();

        log.info("Photos to delete: {}", list.size());

        repository.deleteAll(list);
    }

    @Override
    @Transactional
    public void deletePhotosFromDBWhenStatusISError() {
        String loggedUserId = userQuery.findLoggedUserId();   // raz, nie 2x

        repository.deleteAllById(repository.findAllByStatusAndUserId(Status.ERROR, loggedUserId).stream()
                .map(PhotoAttribute::getId).toList());

        auditPhotoSpringDataRepository.deleteAllById(
                auditPhotoSpringDataRepository.findAllByUserIdAndStatus(loggedUserId, Status.ERROR.name()).stream()
                        .map(AuditPhotos::getId).toList());
    }
}