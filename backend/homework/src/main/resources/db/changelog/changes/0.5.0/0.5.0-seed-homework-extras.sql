-- 0.5.0 demo extras (homework): a rubric-graded assignment and a late-penalty assignment, plus a
-- backfill of the existing FizzBuzz grade's effective_score/max_points so it is no longer the default.

-- ===== (a) Week 2 — Rubric Essay: rubric + criteria + a criteria-graded submission =====
INSERT INTO assignments (id, group_id, lesson_id, title, description, due_at, status, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       NULL, 'Week 2 — Rubric Essay',
       'Write a short essay on Java memory management; graded against a rubric.',
       '2026-03-16 22:00:00+00', 'PUBLISHED', '2026-03-09 09:00:00+00', '2026-03-09 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE title = 'Week 2 — Rubric Essay');

-- The rubric (one optional rubric per assignment).
INSERT INTO rubrics (id, assignment_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM assignments WHERE title = 'Week 2 — Rubric Essay' LIMIT 1),
       '2026-03-09 09:00:00+00', '2026-03-09 09:00:00+00'
WHERE NOT EXISTS (
  SELECT 1 FROM rubrics
  WHERE assignment_id = (SELECT id FROM assignments WHERE title = 'Week 2 — Rubric Essay' LIMIT 1));

-- Point the assignment back at its rubric.
UPDATE assignments
SET rubric_id = (SELECT r.id FROM rubrics r
                   JOIN assignments a ON a.id = r.assignment_id
                  WHERE a.title = 'Week 2 — Rubric Essay' LIMIT 1)
WHERE title = 'Week 2 — Rubric Essay' AND rubric_id IS NULL;

-- Ordered rubric criteria (max total = 30 points).
INSERT INTO rubric_criteria (id, rubric_id, label, max_points, position, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT r.id FROM rubrics r JOIN assignments a ON a.id = r.assignment_id
         WHERE a.title = 'Week 2 — Rubric Essay' LIMIT 1),
       c.label, c.max_points, c.position, '2026-03-09 09:00:00+00', '2026-03-09 09:00:00+00'
FROM (VALUES
  ('Clarity',     10, 0),
  ('Correctness', 15, 1),
  ('Examples',     5, 2)
) AS c(label, max_points, position)
WHERE NOT EXISTS (
  SELECT 1 FROM rubric_criteria x
  WHERE x.label = c.label
    AND x.rubric_id = (SELECT r.id FROM rubrics r JOIN assignments a ON a.id = r.assignment_id
                        WHERE a.title = 'Week 2 — Rubric Essay' LIMIT 1));

-- Sam's on-time submission.
INSERT INTO submissions (id, assignment_id, student_id, version, status, content, submitted_at, late, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM assignments WHERE title = 'Week 2 — Rubric Essay' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       1, 'GRADED', 'The JVM splits memory into the stack and the heap; the GC reclaims the heap...',
       '2026-03-14 18:00:00+00', false, '2026-03-14 18:00:00+00', '2026-03-15 11:00:00+00'
ON CONFLICT DO NOTHING;

-- Criteria-based grade: raw = effective = 25 of 30 (8 + 13 + 4).
INSERT INTO grades (id, submission_id, score, effective_score, max_points, graded_by, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT s.id FROM submissions s JOIN assignments a ON a.id = s.assignment_id
         WHERE a.title = 'Week 2 — Rubric Essay' LIMIT 1),
       25, 25, 30,
       (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       '2026-03-15 11:00:00+00', '2026-03-15 11:00:00+00'
ON CONFLICT DO NOTHING;

-- Per-criterion score breakdown.
INSERT INTO grade_criteria (id, grade_id, criterion_id, points, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT g.id FROM grades g
          JOIN submissions s ON s.id = g.submission_id
          JOIN assignments a ON a.id = s.assignment_id
         WHERE a.title = 'Week 2 — Rubric Essay' LIMIT 1),
       (SELECT rc.id FROM rubric_criteria rc
          JOIN rubrics r ON r.id = rc.rubric_id
          JOIN assignments a ON a.id = r.assignment_id
         WHERE a.title = 'Week 2 — Rubric Essay' AND rc.label = gc.label LIMIT 1),
       gc.points, '2026-03-15 11:00:00+00', '2026-03-15 11:00:00+00'
FROM (VALUES
  ('Clarity',      8),
  ('Correctness', 13),
  ('Examples',     4)
) AS gc(label, points)
WHERE NOT EXISTS (
  SELECT 1 FROM grade_criteria x
  WHERE x.grade_id = (SELECT g.id FROM grades g
                        JOIN submissions s ON s.id = g.submission_id
                        JOIN assignments a ON a.id = s.assignment_id
                       WHERE a.title = 'Week 2 — Rubric Essay' LIMIT 1)
    AND x.criterion_id = (SELECT rc.id FROM rubric_criteria rc
                            JOIN rubrics r ON r.id = rc.rubric_id
                            JOIN assignments a ON a.id = r.assignment_id
                           WHERE a.title = 'Week 2 — Rubric Essay' AND rc.label = gc.label LIMIT 1));

-- ===== (b) Week 1 — Late Lab: late-penalty config + a late, penalized submission =====
INSERT INTO assignments (id, group_id, lesson_id, title, description, due_at, status,
                         late_penalty_pct_per_day, max_late_penalty_pct, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       NULL, 'Week 1 — Late Lab',
       'Build a small CLI tool. Late submissions lose 10% per day, capped at 50%.',
       '2026-03-09 22:00:00+00', 'PUBLISHED', 10, 50, '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE title = 'Week 1 — Late Lab');

-- Sam's late submission — submitted ~2 days after due_at.
INSERT INTO submissions (id, assignment_id, student_id, version, status, content, submitted_at, late, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM assignments WHERE title = 'Week 1 — Late Lab' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       1, 'GRADED', 'public static void main(String[] args) { /* CLI tool */ }',
       '2026-03-11 14:00:00+00', true, '2026-03-11 14:00:00+00', '2026-03-12 10:00:00+00'
ON CONFLICT DO NOTHING;

-- Grade: raw 90, 2 days late x 10% = 20% penalty -> effective 72 of 100 (effective < raw).
INSERT INTO grades (id, submission_id, score, effective_score, max_points, graded_by, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT s.id FROM submissions s JOIN assignments a ON a.id = s.assignment_id
         WHERE a.title = 'Week 1 — Late Lab' LIMIT 1),
       90, 72, 100,
       (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       '2026-03-12 10:00:00+00', '2026-03-12 10:00:00+00'
ON CONFLICT DO NOTHING;

-- ===== Backfill the existing FizzBuzz grade so effective_score/max_points are not the column defaults =====
UPDATE grades
SET effective_score = 85, max_points = 100
WHERE submission_id = (SELECT id FROM submissions WHERE content LIKE 'for (int i%' LIMIT 1);
