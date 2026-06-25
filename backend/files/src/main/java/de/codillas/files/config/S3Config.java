package de.codillas.files.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Builds the S3 client for minio (path-style addressing, endpoint override). Lazy — no connection
 * is made until the first object operation, so the bean is safe to create in every profile.
 */
@Configuration
@EnableConfigurationProperties(FilesProperties.class)
public class S3Config {

  @Bean
  S3Client s3Client(FilesProperties properties) {
    return S3Client.builder()
        .endpointOverride(URI.create(properties.endpoint()))
        .region(Region.of(properties.region()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
        .forcePathStyle(true)
        .build();
  }
}
