package com.homebudget.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public StorageService(S3Client s3Client,
                          @Value("${app.storage.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void putObject(String key, byte[] data, String contentType) {
        logger.debug("Uploading object: bucket={}, key={}, size={}", bucketName, key, data.length);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(data));
        logger.info("Uploaded object: key={}", key);
    }

    public byte[] getObject(String key) {
        logger.debug("Downloading object: bucket={}, key={}", bucketName, key);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            byte[] data = s3Client.getObjectAsBytes(request).asByteArray();
            logger.debug("Downloaded object: key={}, size={}", key, data.length);
            return data;
        } catch (NoSuchKeyException e) {
            logger.warn("Object not found: key={}", key);
            return null;
        }
    }

    public void deleteObject(String key) {
        logger.debug("Deleting object: bucket={}, key={}", bucketName, key);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(request);
        logger.info("Deleted object: key={}", key);
    }

    public void copyObject(String sourceKey, String destKey) {
        logger.debug("Copying object: {} -> {}", sourceKey, destKey);
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destKey)
                .build();
        s3Client.copyObject(request);
        logger.info("Copied object: {} -> {}", sourceKey, destKey);
    }

    public boolean objectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
