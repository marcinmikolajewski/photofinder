package eu.mm.software.photofinder.photosattribute.infrastructure.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketLifecycleArgs;
import io.minio.messages.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket.tmp}")
    private String tmpBucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public CommandLineRunner initMinioBuckets(MinioClient minioClient) {
        return args -> {
            createBucketIfNotExists(minioClient, tmpBucket);
            setupLifecyclePolicy(minioClient, tmpBucket);
        };
    }

    private void createBucketIfNotExists(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());

        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);
        } else {
            log.info("MinIO bucket already exists: {}", bucket);
        }
    }

    private void setupLifecyclePolicy(MinioClient client, String bucket) throws Exception {

        LifecycleRule rule = new LifecycleRule(
                Status.ENABLED,
                null,
                new Expiration((ZonedDateTime) null, 2, null),
                new RuleFilter("photos/"),
                "delete-tmp-after-2-days",
                null,
                null,
                null
        );
        LifecycleConfiguration lifecycleConfig = new LifecycleConfiguration(List.of(rule));

        client.setBucketLifecycle(
                SetBucketLifecycleArgs.builder()
                        .bucket(bucket)
                        .config(lifecycleConfig)
                        .build());

        log.info("Lifecycle policy set for bucket: {}", bucket);
    }
}