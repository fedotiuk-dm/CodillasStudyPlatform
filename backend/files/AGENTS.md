# files — module contract

Local contract for the `files` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** S3/minio file storage — attachments for homework / materials / chat.
- **Key entities:** StoredFile (metadata)
- **Publishes:** —
- **Consumes:** —
- **Depends on (by id / events / API only):** minio (S3)
- **OpenAPI spec:** `backend/openapi/files-paths.yaml` (+ `files-schemas.yaml`)
- **Status:** skeleton — implement in phase 1 (copy from boosting).
