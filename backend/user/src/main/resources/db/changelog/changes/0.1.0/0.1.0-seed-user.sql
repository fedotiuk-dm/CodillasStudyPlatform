-- Demo profiles for the three seeded Keycloak users (admin / teacher / student).
-- The user_id values are the pinned Keycloak subjects (see backend/keycloak/realm-export.json) —
-- they are real external identifiers, not invented. Everything else keys off display_name.
INSERT INTO user_profiles (id, user_id, display_name, bio, created_at, updated_at) VALUES
  (gen_random_uuid(), 'cc1fde04-3c8c-4dc1-8b94-c6e0289389c7', 'Ada Admin',     'Platform administrator.',     '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'),
  (gen_random_uuid(), '6cbdfc12-d06c-4c6f-aefb-c62e9594ce27', 'Tom Teacher', 'Senior Java instructor.',     '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'),
  (gen_random_uuid(), '93552bf7-1c20-4667-b352-11c6dfda7fbb', 'Sam Student',  'Aspiring backend developer.', '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00')
ON CONFLICT (user_id) DO NOTHING;
