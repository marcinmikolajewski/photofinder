package eu.mm.software.photofinder.photosattribute.infrastructure.starage;

import eu.mm.software.photofinder.photosattribute.domain.ImageNotFoundException;
import eu.mm.software.photofinder.photosattribute.domain.TemporaryImageStorage;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioTemporaryImageStorage implements TemporaryImageStorage {

    private final MinioClient minioClient;

    @Value("${minio.bucket.tmp}")
    private String bucket;

    @Override
    public String save(String userId, String photoId, byte[] imageData) {
        String imageRef = buildImageRef(userId, photoId);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(imageRef)
                    .stream(new ByteArrayInputStream(imageData), imageData.length, -1)
                    .contentType("image/jpeg")
                    .build());
            log.info("Saved tmp image: {}", imageRef);
            return imageRef;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save image to MinIO: " + imageRef, e);
        }
    }

    @Override
    public byte[] load(String imageRef) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(imageRef)
                    .build())
                    .readAllBytes();
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new ImageNotFoundException(imageRef);
            }
            throw new RuntimeException("Failed to load image from MinIO: " + imageRef, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load image from MinIO: " + imageRef, e);
        }
    }

    @Override
    public void delete(String imageRef) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(imageRef)
                    .build());
            log.info("Deleted tmp image: {}", imageRef);
        } catch (Exception e) {
            log.warn("Failed to delete tmp image: {}", imageRef, e);
        }
    }

    @Override
    public void deleteAllForUser(String userId) {
        String prefix = "photos/" + userId + "/tmp/";
        try {
            minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build())
                    .forEach(item -> {
                        try {
                            minioClient.removeObject(RemoveObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(item.get().objectName())
                                    .build());
                        } catch (Exception e) {
                            log.warn("Failed to delete: {}", item, e);
                        }
                    });
            log.info("Deleted all tmp images for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to list objects for user: {}", userId, e);
        }
    }

    private String buildImageRef(String userId, String photoId) {
        return "photos/" + userId + "/tmp/" + photoId + ".jpg";
    }
}