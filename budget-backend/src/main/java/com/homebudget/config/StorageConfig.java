package com.homebudget.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Configuration
public class StorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Value("${app.storage.access-key}")
    private String accessKey;

    @Value("${app.storage.secret-key}")
    private String secretKey;

    @Value("${app.storage.region:us-east-1}")
    private String region;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.builder().build())
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();

        ensureBucketExists(client);
        return client;
    }

    private void ensureBucketExists(S3Client client) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            logger.info("Storage bucket '{}' exists", bucketName);
        } catch (NoSuchBucketException e) {
            logger.info("Creating storage bucket '{}'", bucketName);
            client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            logger.info("Storage bucket '{}' created", bucketName);
        } catch (Exception e) {
            logger.warn("Could not verify storage bucket '{}': {}", bucketName, e.getMessage());
        }
    }
}
