-- The course demo seed authors a FILE material pointing at this fixed id. Without the matching
-- stored_files row the material was unopenable and the reference dangled from day one.
-- No bytes are uploaded — the metadata row exists so the demo data is at least self-consistent.
INSERT INTO stored_files (id, storage_key, original_filename, content_type, file_size,
                          uploaded_by, reference_type, reference_id, created_at, updated_at,
                          lock_version)
SELECT '11111111-1111-1111-1111-111111111111'::uuid,
       'seed/lecture-slides.pdf',
       'lecture-slides.pdf',
       'application/pdf',
       0,
       (SELECT user_id FROM user_profiles ORDER BY created_at LIMIT 1),
       'MATERIAL',
       NULL,
       '2026-03-02 09:00:00+00',
       '2026-03-02 09:00:00+00',
       0
WHERE EXISTS (SELECT 1 FROM user_profiles)
  AND NOT EXISTS (SELECT 1 FROM stored_files
                  WHERE id = '11111111-1111-1111-1111-111111111111'::uuid);
