-- Two notifications for the student plus the group roster row (read model).
INSERT INTO notification_memberships (id, group_id, student_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO notifications (id, recipient_id, type, title, body, reference_id, read, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       n.type, n.title, n.body,
       (SELECT id FROM assignments WHERE title = 'Week 1 — FizzBuzz' LIMIT 1),
       n.read, n.created_at, n.created_at
FROM (VALUES
  ('ASSIGNMENT_PUBLISHED', 'New assignment: Week 1 — FizzBuzz', 'A new assignment was published in Java Cohort A.', false, TIMESTAMPTZ '2026-03-02 09:10:00+00'),
  ('SUBMISSION_GRADED',    'Your submission was graded',         'You scored 85 on Week 1 — FizzBuzz.',             true,  TIMESTAMPTZ '2026-03-07 10:00:00+00')
) AS n(type, title, body, read, created_at)
ON CONFLICT DO NOTHING;
