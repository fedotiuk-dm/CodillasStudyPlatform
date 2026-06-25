-- One published assignment with a graded submission (review + grade).
INSERT INTO assignments (id, group_id, lesson_id, title, description, due_at, status, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       NULL, 'Week 1 — FizzBuzz', 'Implement FizzBuzz and submit your solution as plain text.',
       '2026-03-09 22:00:00+00', 'PUBLISHED', '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO submissions (id, assignment_id, student_id, version, status, content, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM assignments WHERE title = 'Week 1 — FizzBuzz' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       1, 'GRADED', 'for (int i = 1; i <= 100; i++) { ... FizzBuzz ... }',
       '2026-03-06 12:00:00+00', '2026-03-07 10:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO reviews (id, submission_id, reviewer_id, comment, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM submissions WHERE content LIKE 'for (int i%' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       'Solid solution — watch the edge case for multiples of 15.', '2026-03-07 09:30:00+00', '2026-03-07 09:30:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO grades (id, submission_id, score, graded_by, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM submissions WHERE content LIKE 'for (int i%' LIMIT 1),
       85, (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       '2026-03-07 10:00:00+00', '2026-03-07 10:00:00+00'
ON CONFLICT DO NOTHING;
