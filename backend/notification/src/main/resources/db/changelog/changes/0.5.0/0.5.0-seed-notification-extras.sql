-- 0.5.0 demo extras (notification): backfill render params on the existing rows + add unread
-- notifications (ATTEMPT_COMPLETED, ASSIGNMENT_DUE_SOON, DIRECT_MESSAGE) so the bell shows several
-- unread with working i18n params. The frontend re-renders body text from type + params.

-- Backfill the {points} token on the existing graded-submission notification.
UPDATE notifications
SET params = '{"points":"85"}'::jsonb
WHERE type = 'SUBMISSION_GRADED'
  AND recipient_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1);

-- The existing assignment-published notification takes no params; give it an empty object (not null).
UPDATE notifications
SET params = '{}'::jsonb
WHERE type = 'ASSIGNMENT_PUBLISHED'
  AND recipient_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1)
  AND params IS NULL;

-- Unread: a graded test attempt (params {points}), deep-linked to the control-test attempt.
INSERT INTO notifications (id, recipient_id, type, title, body, reference_id, params, read, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'ATTEMPT_COMPLETED', 'Test scored', 'Your test attempt scored 2 points.',
       (SELECT a.id FROM attempts a JOIN tests t ON t.id = a.test_id
         WHERE t.title = 'Java Control Test'
           AND a.student_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1) LIMIT 1),
       '{"points":"2"}'::jsonb, false, '2026-03-15 10:20:00+00', '2026-03-15 10:20:00+00'
WHERE NOT EXISTS (
  SELECT 1 FROM notifications
  WHERE type = 'ATTEMPT_COMPLETED'
    AND recipient_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1));

-- Unread: an assignment-due-soon reminder, deep-linked to the rubric essay (due 2026-03-16).
INSERT INTO notifications (id, recipient_id, type, title, body, reference_id, params, read, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'ASSIGNMENT_DUE_SOON', 'Assignment due soon', 'An assignment in your group is due soon.',
       (SELECT id FROM assignments WHERE title = 'Week 2 — Rubric Essay' LIMIT 1),
       '{}'::jsonb, false, '2026-03-15 09:00:00+00', '2026-03-15 09:00:00+00'
WHERE NOT EXISTS (
  SELECT 1 FROM notifications
  WHERE type = 'ASSIGNMENT_DUE_SOON'
    AND recipient_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1));

-- Unread: a direct message, deep-linked to the DIRECT chat room.
INSERT INTO notifications (id, recipient_id, type, title, body, reference_id, params, read, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       'DIRECT_MESSAGE', 'New message', 'You have a new chat message.',
       (SELECT id FROM chat_rooms WHERE name = 'DM Tom-Sam' AND type = 'DIRECT' LIMIT 1),
       '{}'::jsonb, false, '2026-03-16 12:00:00+00', '2026-03-16 12:00:00+00'
WHERE NOT EXISTS (
  SELECT 1 FROM notifications
  WHERE type = 'DIRECT_MESSAGE'
    AND recipient_id = (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1));
