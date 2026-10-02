package eu.mm.software.photofinder.worker.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerMinioStorage {

    private final MinioClient minioClient;

    @Value("${minio.bucket.tmp}")
    private String bucket;

    public byte[] load(String imageRef) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(imageRef)
                    .build())
                    .readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load image from MinIO: " + imageRef, e);
        }
    }

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
}
