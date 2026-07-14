# files — module contract

Local contract for the `files` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** S3/minio file storage — attachments for homework / materials / chat.
- **Key entities:** StoredFile (metadata)
- **Publishes:** —
- **Consumes:** —
- **Depends on (by id / events / API only):** minio (S3)
- **OpenAPI spec:** `backend/openapi/files-paths.yaml` (+ `files-schemas.yaml`)
- **Status:** implemented (v0.1.0 S3/minio via backend proxy).

## Conventions

- **Backend-proxied object storage.** Upload (multipart) and download stream through the backend;
  the bytes live in S3/minio under a random `storageKey`, `StoredFile` holds only metadata
  (filename, content-type, size, uploader, optional `referenceType`/`referenceId`). No presigned
  URLs in v1.
- **`ObjectStorage` port** (`S3ObjectStorage` impl) isolates the AWS SDK; the `S3Client` bean
  (`S3Config`, `codillas.files.*`, minio path-style) is **lazy** — it builds in every profile but
  only connects on a real operation, so non-files contexts boot without minio. The bucket is created
  by infra/tests, not the app.
- Delete is `@RequiresTeacher`; upload/metadata are `@RequiresAuthenticated`. **Download is
  object-level authorized:** `FileServiceImpl.download` resolves a `FileAccessAuthorizer` SPI by the
  file's `FileReferenceType` and returns 404 (never leaks existence) when the caller may not read it.
  Each owning module implements the SPI once — `homework` (HOMEWORK → owner-or-staff), `chat` (CHAT →
  room member), `files` (MATERIAL → any authenticated). The SPI receives only ids, never the
  `StoredFile` entity, so implementers import no files persistence type. A file with no matching
  authorizer (loose upload) is readable only by its uploader or staff. Integration-tested end-to-end
  against a MinIO Testcontainer.
