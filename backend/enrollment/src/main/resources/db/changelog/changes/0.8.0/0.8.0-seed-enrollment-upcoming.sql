-- Scheduled relative to seeding time so the dashboard always has an upcoming lesson.
INSERT INTO scheduled_lessons (id, group_id, title, scheduled_at, meet_link, recording_url, lesson_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       'Lesson 3 — Methods',
       date_trunc('day', now()) + interval '2 days 17 hours',
       'https://meet.example.com/java-a-3',
       NULL,
       (SELECT id FROM lessons WHERE title = 'Methods' LIMIT 1),
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM scheduled_lessons WHERE title = 'Lesson 3 — Methods');
