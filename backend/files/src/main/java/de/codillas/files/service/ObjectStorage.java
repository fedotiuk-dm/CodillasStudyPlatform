package de.codillas.files.service;

/** Object-storage port — stores and retrieves file bytes by key. Backed by S3/minio. */
public interface ObjectStorage {

  void put(String key, byte[] content, String contentType);

  byte[] get(String key);

  void delete(String key);
}
