package de.codillas.files.service;

import org.springframework.stereotype.Component;

import de.codillas.files.config.FilesProperties;

import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/** {@link ObjectStorage} backed by S3/minio. */
@Component
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage {

  private final S3Client s3Client;
  private final FilesProperties properties;

  @Override
  public void put(String key, byte[] content, String contentType) {
    s3Client.putObject(
        request -> request.bucket(properties.bucket()).key(key).contentType(contentType),
        RequestBody.fromBytes(content));
  }

  @Override
  public byte[] get(String key) {
    ResponseBytes<GetObjectResponse> response =
        s3Client.getObjectAsBytes(request -> request.bucket(properties.bucket()).key(key));
    return response.asByteArray();
  }

  @Override
  public void delete(String key) {
    s3Client.deleteObject(request -> request.bucket(properties.bucket()).key(key));
  }
}
