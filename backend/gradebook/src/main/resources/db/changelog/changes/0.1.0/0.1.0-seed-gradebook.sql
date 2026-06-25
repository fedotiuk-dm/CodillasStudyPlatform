-- Gradebook is an event-fed read model; we seed it directly so the demo gradebook isn't empty.
INSERT INTO gradebook_memberships (id, group_id, student_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO progress_entries (id, student_id, source, source_id, reference_id, score, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'HOMEWORK',
       (SELECT id FROM submissions WHERE content LIKE 'for (int i%' LIMIT 1),
       (SELECT id FROM assignments WHERE title = 'Week 1 — FizzBuzz' LIMIT 1),
       85, '2026-03-07 10:00:00+00', '2026-03-07 10:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO progress_entries (id, student_id, source, source_id, reference_id, score, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'TEST',
       (SELECT id FROM attempts WHERE status = 'GRADED' LIMIT 1),
       (SELECT id FROM tests WHERE title = 'Java Basics Quiz' LIMIT 1),
       2, '2026-03-08 14:10:00+00', '2026-03-08 14:10:00+00'
ON CONFLICT DO NOTHING;
