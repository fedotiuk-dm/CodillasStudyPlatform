-- Demo recipient emails; the same addresses as backend/keycloak/realm-export.json.
INSERT INTO recipient_email (id, user_id, email, created_at, updated_at)
SELECT gen_random_uuid(), v.user_id::uuid, v.email, now(), now()
FROM (VALUES
  ('cc1fde04-3c8c-4dc1-8b94-c6e0289389c7', 'admin@codillas.dev'),
  ('6cbdfc12-d06c-4c6f-aefb-c62e9594ce27', 'teacher@codillas.dev'),
  ('93552bf7-1c20-4667-b352-11c6dfda7fbb', 'student@codillas.dev')
) AS v(user_id, email)
ON CONFLICT (user_id) DO NOTHING;
