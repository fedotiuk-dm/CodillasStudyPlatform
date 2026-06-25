-- One published test with three questions, plus the student's graded attempt.
INSERT INTO tests (id, lesson_id, title, status, created_at, updated_at)
SELECT gen_random_uuid(), NULL, 'Java Basics Quiz', 'PUBLISHED', '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO questions (id, test_id, type, prompt, points, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), (SELECT id FROM tests WHERE title = 'Java Basics Quiz' LIMIT 1), t.type, t.prompt, t.points, t.sort_order, '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
FROM (VALUES
  ('SINGLE_CHOICE', 'Which keyword declares a constant (final variable) in Java?', 1, 0),
  ('TRUE_FALSE',    'Java is a statically typed language.',                       1, 1),
  ('SHORT_TEXT',    'Name the method that is a Java program''s entry point.',      2, 2)
) AS t(type, prompt, points, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO options (id, question_id, text, correct, position, created_at, updated_at)
SELECT gen_random_uuid(), (SELECT id FROM questions WHERE prompt = o.prompt LIMIT 1), o.text, o.correct, o.position, '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
FROM (VALUES
  ('Which keyword declares a constant (final variable) in Java?', 'final',  true,  0),
  ('Which keyword declares a constant (final variable) in Java?', 'const',  false, 1),
  ('Which keyword declares a constant (final variable) in Java?', 'static', false, 2),
  ('Which keyword declares a constant (final variable) in Java?', 'val',    false, 3),
  ('Java is a statically typed language.',                        'True',   true,  0),
  ('Java is a statically typed language.',                        'False',  false, 1)
) AS o(prompt, text, correct, position)
ON CONFLICT DO NOTHING;

-- The student's graded attempt: both choice questions correct (2 pts), short-text pending (0).
INSERT INTO attempts (id, test_id, student_id, status, score, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM tests WHERE title = 'Java Basics Quiz' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'GRADED', 2, '2026-03-08 14:00:00+00', '2026-03-08 14:10:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO answers (id, attempt_id, question_id, text, awarded_points, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM attempts WHERE status = 'GRADED' AND test_id = (SELECT id FROM tests WHERE title = 'Java Basics Quiz' LIMIT 1) LIMIT 1),
       (SELECT id FROM questions WHERE prompt = a.prompt LIMIT 1), a.text, a.awarded_points,
       '2026-03-08 14:10:00+00', '2026-03-08 14:10:00+00'
FROM (VALUES
  ('Which keyword declares a constant (final variable) in Java?', CAST(NULL AS text), 1),
  ('Java is a statically typed language.',                        CAST(NULL AS text), 1),
  ('Name the method that is a Java program''s entry point.',      'main',             0)
) AS a(prompt, text, awarded_points)
ON CONFLICT DO NOTHING;

-- The selected options for the two choice answers (the correct ones).
INSERT INTO answer_selected_options (answer_id, option_id)
SELECT ans.id, opt.id
FROM answers ans
JOIN questions q ON q.id = ans.question_id
JOIN options opt ON opt.question_id = q.id AND opt.correct = true
WHERE q.type IN ('SINGLE_CHOICE', 'TRUE_FALSE')
  AND ans.attempt_id = (SELECT id FROM attempts WHERE status = 'GRADED' LIMIT 1)
ON CONFLICT DO NOTHING;
