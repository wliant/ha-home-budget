package com.homebudget.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import org.springframework.beans.factory.annotation.Value;

/**
 * Health indicator that verifies MinIO/S3 storage connectivity.
 * Registered with Spring Boot Actuator and exposed via /actuator/health.
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(StorageHealthIndicator.class);

    private final S3Client s3Client;
    private final String bucketName;

    public StorageHealthIndicator(S3Client s3Client,
                                  @Value("${app.storage.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public Health health() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            return Health.up()
                    .withDetail("bucket", bucketName)
                    .build();
        } catch (Exception e) {
            logger.warn("Storage health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("bucket", bucketName)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
