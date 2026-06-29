-- 0.5.0 demo extras (gradebook): weight + scope the two existing progress entries, then add rows for
-- the new graded items so the weighted course grade (Sum awarded / Sum max) and the homework-vs-test
-- breakdown render. The gradebook is an event-fed read model; the demo seeds it directly.

-- Scope + weight the existing FizzBuzz homework entry (85 of 100).
UPDATE progress_entries
SET max_points = 100,
    group_id = (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1)
WHERE source = 'HOMEWORK'
  AND reference_id = (SELECT id FROM assignments WHERE title = 'Week 1 — FizzBuzz' LIMIT 1);

-- Scope + weight the existing Java Basics Quiz test entry (2 of 4).
UPDATE progress_entries
SET max_points = 4,
    group_id = (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1)
WHERE source = 'TEST'
  AND reference_id = (SELECT id FROM tests WHERE title = 'Java Basics Quiz' LIMIT 1);

-- New homework entry: Week 2 — Rubric Essay (25 of 30).
INSERT INTO progress_entries (id, student_id, source, source_id, reference_id, score, max_points, group_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'HOMEWORK',
       (SELECT s.id FROM submissions s JOIN assignments a ON a.id = s.assignment_id
         WHERE a.title = 'Week 2 — Rubric Essay' LIMIT 1),
       (SELECT id FROM assignments WHERE title = 'Week 2 — Rubric Essay' LIMIT 1),
       25, 30, (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       '2026-03-15 11:00:00+00', '2026-03-15 11:00:00+00'
ON CONFLICT DO NOTHING;

-- New homework entry: Week 1 — Late Lab (effective 72 of 100).
INSERT INTO progress_entries (id, student_id, source, source_id, reference_id, score, max_points, group_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'HOMEWORK',
       (SELECT s.id FROM submissions s JOIN assignments a ON a.id = s.assignment_id
         WHERE a.title = 'Week 1 — Late Lab' LIMIT 1),
       (SELECT id FROM assignments WHERE title = 'Week 1 — Late Lab' LIMIT 1),
       72, 100, (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       '2026-03-12 10:00:00+00', '2026-03-12 10:00:00+00'
ON CONFLICT DO NOTHING;

-- New test entry: Java Control Test (2 of 4).
INSERT INTO progress_entries (id, student_id, source, source_id, reference_id, score, max_points, group_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'TEST',
       (SELECT a.id FROM attempts a JOIN tests t ON t.id = a.test_id
         WHERE t.title = 'Java Control Test'
           AND a.student_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1) LIMIT 1),
       (SELECT id FROM tests WHERE title = 'Java Control Test' LIMIT 1),
       2, 4, (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       '2026-03-15 10:20:00+00', '2026-03-15 10:20:00+00'
ON CONFLICT DO NOTHING;
