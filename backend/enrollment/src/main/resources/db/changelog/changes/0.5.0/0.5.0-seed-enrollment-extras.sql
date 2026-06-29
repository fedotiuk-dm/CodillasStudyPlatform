-- 0.5.0 demo extras (enrollment): a DRAFT cohort and an ARCHIVED cohort, plus wiring Cohort A's
-- scheduled lessons to the new course lessonIds. All FKs resolve by natural key — no hard-coded uuids.

-- A DRAFT cohort on the same course, taught by Tom, with no members yet.
INSERT INTO study_groups (id, name, course_id, teacher_id, start_date, status, created_at, updated_at)
SELECT gen_random_uuid(), 'Java Cohort B',
       (SELECT id FROM courses WHERE name = 'Java Fundamentals' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       DATE '2026-04-06', 'DRAFT', '2026-03-20 09:00:00+00', '2026-03-20 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM study_groups WHERE name = 'Java Cohort B');

-- An ARCHIVED cohort — a past run of the course.
INSERT INTO study_groups (id, name, course_id, teacher_id, start_date, status, created_at, updated_at)
SELECT gen_random_uuid(), 'Java Cohort 2024',
       (SELECT id FROM courses WHERE name = 'Java Fundamentals' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       DATE '2024-09-02', 'ARCHIVED', '2024-09-01 09:00:00+00', '2024-12-20 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM study_groups WHERE name = 'Java Cohort 2024');

-- Wire Cohort A's existing scheduled lesson to the course lesson "Variables & Types".
UPDATE scheduled_lessons
SET lesson_id = (SELECT id FROM lessons WHERE title = 'Variables & Types' LIMIT 1)
WHERE title = 'Lesson 1 — Variables & Types'
  AND group_id = (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1);

-- A second Cohort A scheduled lesson, linked to the course lesson "Control Flow".
INSERT INTO scheduled_lessons (id, group_id, title, scheduled_at, meet_link, recording_url, lesson_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       'Lesson 2 — Control Flow', '2026-03-11 17:00:00+00', 'https://meet.example.com/java-a-2', NULL,
       (SELECT id FROM lessons WHERE title = 'Control Flow' LIMIT 1),
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
WHERE NOT EXISTS (
  SELECT 1 FROM scheduled_lessons
  WHERE title = 'Lesson 2 — Control Flow'
    AND group_id = (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1));
