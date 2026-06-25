-- One cohort running the Java course, the student enrolled, one lesson held + attended.
-- All FKs resolve by natural key (course/group/lesson name, user display name) — no UUIDs hard-coded.
INSERT INTO study_groups (id, name, course_id, teacher_id, start_date, created_at, updated_at)
SELECT gen_random_uuid(), 'Java Cohort A',
       (SELECT id FROM courses WHERE name = 'Java Fundamentals' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       DATE '2026-03-02', '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO group_members (id, group_id, user_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO scheduled_lessons (id, group_id, title, scheduled_at, meet_link, recording_url, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       'Lesson 1 — Variables & Types', '2026-03-04 17:00:00+00', 'https://meet.example.com/java-a-1', NULL,
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO attendance (id, scheduled_lesson_id, user_id, present, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM scheduled_lessons WHERE title = 'Lesson 1 — Variables & Types' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       true, '2026-03-04 17:05:00+00', '2026-03-04 17:05:00+00'
ON CONFLICT DO NOTHING;
