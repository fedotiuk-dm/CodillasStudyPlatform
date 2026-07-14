-- 0.5.0 demo extras (chat): a DIRECT room between Tom and Sam with a couple of messages, so the
-- DIRECT_MESSAGE notification can deep-link to a real room.
INSERT INTO chat_rooms (id, type, name, reference_id, created_at, updated_at)
SELECT gen_random_uuid(), 'DIRECT', 'DM Tom-Sam', NULL, '2026-03-10 09:00:00+00', '2026-03-10 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM chat_rooms WHERE name = 'DM Tom-Sam' AND type = 'DIRECT');

INSERT INTO chat_room_members (id, room_id, user_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM chat_rooms WHERE name = 'DM Tom-Sam' AND type = 'DIRECT' LIMIT 1),
       p.user_id, '2026-03-10 09:00:00+00', '2026-03-10 09:00:00+00'
FROM user_profiles p
WHERE p.display_name IN ('Tom Teacher', 'Sam Student')
ON CONFLICT DO NOTHING;

INSERT INTO chat_messages (id, room_id, sender_id, content, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM chat_rooms WHERE name = 'DM Tom-Sam' AND type = 'DIRECT' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = m.sender LIMIT 1),
       m.content, m.created_at, m.created_at
FROM (VALUES
  ('Tom Teacher', 'Hi Sam — great progress on the labs!',                  TIMESTAMPTZ '2026-03-16 11:55:00+00'),
  ('Sam Student', 'Thanks Tom! I had a question about the rubric essay.',  TIMESTAMPTZ '2026-03-16 11:58:00+00')
) AS m(sender, content, created_at)
WHERE NOT EXISTS (
  SELECT 1 FROM chat_messages x
  WHERE x.content = m.content
    AND x.room_id = (SELECT id FROM chat_rooms WHERE name = 'DM Tom-Sam' AND type = 'DIRECT' LIMIT 1));
