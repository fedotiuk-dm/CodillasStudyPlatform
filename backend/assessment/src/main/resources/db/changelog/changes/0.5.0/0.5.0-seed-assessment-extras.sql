-- 0.5.0 demo extras (assessment): a fully configured "Java Control Test" (attempt cap, timer,
-- availability window, shuffle) with a MULTIPLE_CHOICE question and Sam's partial-credit attempt;
-- plus attempt_number/started_at set on the existing Java Basics Quiz attempt.

-- Configured test, linked to the "Control Flow" course lesson.
INSERT INTO tests (id, lesson_id, title, status, max_attempts, duration_minutes,
                   available_from, available_until, shuffle_questions, shuffle_options, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM lessons WHERE title = 'Control Flow' LIMIT 1),
       'Java Control Test', 'PUBLISHED', 2, 30,
       '2026-03-14 09:00:00+00', '2026-03-21 22:00:00+00', true, true,
       '2026-03-13 09:00:00+00', '2026-03-13 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM tests WHERE title = 'Java Control Test');

-- A MULTIPLE_CHOICE question worth 4 points.
INSERT INTO questions (id, test_id, type, prompt, points, sort_order, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM tests WHERE title = 'Java Control Test' LIMIT 1),
       'MULTIPLE_CHOICE', 'Which of the following are checked exceptions in Java?', 4, 0,
       '2026-03-13 09:00:00+00', '2026-03-13 09:00:00+00'
WHERE NOT EXISTS (
  SELECT 1 FROM questions
  WHERE prompt = 'Which of the following are checked exceptions in Java?'
    AND test_id = (SELECT id FROM tests WHERE title = 'Java Control Test' LIMIT 1));

-- Four options: two correct (checked exceptions), two incorrect (runtime exceptions).
INSERT INTO options (id, question_id, text, correct, position, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM questions WHERE prompt = 'Which of the following are checked exceptions in Java?' LIMIT 1),
       o.text, o.correct, o.position, '2026-03-13 09:00:00+00', '2026-03-13 09:00:00+00'
FROM (VALUES
  ('IOException',                    true,  0),
  ('SQLException',                   true,  1),
  ('NullPointerException',           false, 2),
  ('ArrayIndexOutOfBoundsException', false, 3)
) AS o(text, correct, position)
WHERE NOT EXISTS (
  SELECT 1 FROM options x
  WHERE x.text = o.text
    AND x.question_id = (SELECT id FROM questions WHERE prompt = 'Which of the following are checked exceptions in Java?' LIMIT 1));

-- Sam's attempt: attempt 1, started, GRADED with partial credit (2 of 4).
INSERT INTO attempts (id, test_id, student_id, status, score, attempt_number, started_at, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM tests WHERE title = 'Java Control Test' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'GRADED', 2, 1, '2026-03-15 10:00:00+00', '2026-03-15 10:00:00+00', '2026-03-15 10:20:00+00'
ON CONFLICT DO NOTHING;

-- The answer: partial credit (some but not all correct options selected) -> awarded 2 of 4.
INSERT INTO answers (id, attempt_id, question_id, text, awarded_points, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT a.id FROM attempts a JOIN tests t ON t.id = a.test_id
         WHERE t.title = 'Java Control Test'
           AND a.student_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1) LIMIT 1),
       (SELECT id FROM questions WHERE prompt = 'Which of the following are checked exceptions in Java?' LIMIT 1),
       NULL, 2, '2026-03-15 10:20:00+00', '2026-03-15 10:20:00+00'
ON CONFLICT DO NOTHING;

-- Sam selected only IOException (one of two correct options) -> partial credit.
INSERT INTO answer_selected_options (answer_id, option_id)
SELECT ans.id, opt.id
FROM answers ans
JOIN questions q ON q.id = ans.question_id
JOIN options opt ON opt.question_id = q.id
WHERE q.prompt = 'Which of the following are checked exceptions in Java?'
  AND opt.text = 'IOException'
  AND ans.attempt_id = (SELECT a.id FROM attempts a JOIN tests t ON t.id = a.test_id
                         WHERE t.title = 'Java Control Test'
                           AND a.student_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1) LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM answer_selected_options aso WHERE aso.answer_id = ans.id AND aso.option_id = opt.id);

-- Set attempt_number/started_at on the existing Java Basics Quiz attempt (started 5 min before grading).
UPDATE attempts
SET attempt_number = 1, started_at = '2026-03-08 13:55:00+00'
WHERE test_id = (SELECT id FROM tests WHERE title = 'Java Basics Quiz' LIMIT 1)
  AND student_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1);
