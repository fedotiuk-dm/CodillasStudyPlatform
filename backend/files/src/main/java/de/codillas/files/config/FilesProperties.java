package de.codillas.files.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Object-storage settings (S3/minio). Bound from {@code codillas.files.*}. */
@ConfigurationProperties("codillas.files")
public record FilesProperties(
    String bucket, String endpoint, String region, String accessKey, String secretKey) {}
