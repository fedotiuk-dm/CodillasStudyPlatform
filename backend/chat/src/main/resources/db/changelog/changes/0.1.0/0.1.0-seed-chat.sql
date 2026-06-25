-- A group chat room for the cohort, with the teacher and student as members and a few messages.
INSERT INTO chat_rooms (id, type, name, reference_id, created_at, updated_at)
SELECT gen_random_uuid(), 'GROUP', 'Java Cohort A',
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO chat_room_members (id, room_id, user_id, created_at, updated_at)
SELECT gen_random_uuid(), (SELECT id FROM chat_rooms WHERE name = 'Java Cohort A' LIMIT 1), p.user_id, '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
FROM user_profiles p
WHERE p.display_name IN ('Tom Teacher', 'Sam Student')
ON CONFLICT DO NOTHING;

INSERT INTO chat_messages (id, room_id, sender_id, content, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM chat_rooms WHERE name = 'Java Cohort A' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = m.sender LIMIT 1),
       m.content, m.created_at, m.created_at
FROM (VALUES
  ('Tom Teacher', 'Welcome to Java Cohort A! 👋',                 TIMESTAMPTZ '2026-03-02 09:05:00+00'),
  ('Sam Student',  'Thanks! Excited to get started.',             TIMESTAMPTZ '2026-03-02 09:07:00+00'),
  ('Tom Teacher', 'Our first lesson is Wednesday at 17:00.',      TIMESTAMPTZ '2026-03-02 09:08:00+00')
) AS m(sender, content, created_at)
ON CONFLICT DO NOTHING;
