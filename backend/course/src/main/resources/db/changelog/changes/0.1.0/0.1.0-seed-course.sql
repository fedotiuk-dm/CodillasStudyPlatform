-- Two demo courses in the catalogue.
INSERT INTO courses (id, name, description, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Java Fundamentals', 'Core Java: syntax, OOP, collections and the standard library.', '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'),
  (gen_random_uuid(), 'Web Development',   'HTTP, REST APIs and building a Spring Boot + Next.js app.',     '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00')
ON CONFLICT DO NOTHING;
