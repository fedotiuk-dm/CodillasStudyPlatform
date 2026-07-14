-- 0.5.0 demo extras (course): full STRUCTURE for "Java Fundamentals" (sections → lessons → materials)
-- plus a DRAFT and an ARCHIVED course so the lifecycle states are visible. Everything keys off natural
-- names; FILE materials carry a placeholder fileId (no real upload is performed by the seed).

-- (b) Two extra catalogue courses exercising the lifecycle states.
INSERT INTO courses (id, name, description, status, created_at, updated_at)
SELECT gen_random_uuid(), 'Algorithms 101',
       'Sorting, searching, complexity and the core data structures.', 'DRAFT',
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'Algorithms 101');

INSERT INTO courses (id, name, description, status, created_at, updated_at)
SELECT gen_random_uuid(), 'Legacy Bootcamp 2024',
       'The 2024 bootcamp — archived, kept for records.', 'ARCHIVED',
       '2024-09-01 09:00:00+00', '2024-12-15 09:00:00+00'
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'Legacy Bootcamp 2024');

-- (a) Structure for "Java Fundamentals": two sections.
INSERT INTO sections (id, course_id, title, sort_order, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM courses WHERE name = 'Java Fundamentals' LIMIT 1),
       s.title, s.sort_order, '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
FROM (VALUES
  ('Getting Started',      0),
  ('Object-Oriented Java', 1)
) AS s(title, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM sections x
  WHERE x.title = s.title
    AND x.course_id = (SELECT id FROM courses WHERE name = 'Java Fundamentals' LIMIT 1));

-- Two lessons per section (with meeting_url / recording_url + sort_order).
INSERT INTO lessons (id, section_id, title, summary, meeting_url, recording_url, sort_order, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT s.id FROM sections s
          JOIN courses c ON c.id = s.course_id
         WHERE c.name = 'Java Fundamentals' AND s.title = l.section_title LIMIT 1),
       l.title, l.summary, l.meeting_url, l.recording_url, l.sort_order,
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
FROM (VALUES
  ('Getting Started',      'Variables & Types', 'Primitives, references, literals and type inference.',
     'https://meet.example.com/java-fundamentals/variables', 'https://video.example.com/jf/variables', 0),
  ('Getting Started',      'Control Flow',      'if/else, switch, loops and branching.',
     'https://meet.example.com/java-fundamentals/control-flow', CAST(NULL AS text), 1),
  ('Object-Oriented Java', 'Classes & Objects', 'Encapsulation, constructors and methods.',
     'https://meet.example.com/java-fundamentals/classes', CAST(NULL AS text), 0),
  ('Object-Oriented Java', 'Collections',       'List, Set, Map and iteration.',
     'https://meet.example.com/java-fundamentals/collections', CAST(NULL AS text), 1)
) AS l(section_title, title, summary, meeting_url, recording_url, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM lessons x
  WHERE x.title = l.title
    AND x.section_id = (SELECT s.id FROM sections s
                          JOIN courses c ON c.id = s.course_id
                         WHERE c.name = 'Java Fundamentals' AND s.title = l.section_title LIMIT 1));

-- Three materials: a mix of LINK (url) and FILE (placeholder fileId).
INSERT INTO materials (id, lesson_id, type, title, url, file_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM lessons WHERE title = m.lesson_title LIMIT 1),
       m.type, m.title, m.url, m.file_id, m.sort_order,
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
FROM (VALUES
  ('Variables & Types', 'LINK', 'Oracle: Java Language Basics',
     'https://docs.oracle.com/javase/tutorial/java/nutsandbolts/', CAST(NULL AS uuid), 0),
  ('Variables & Types', 'FILE', 'Lecture slides (PDF)',
     CAST(NULL AS text), CAST('11111111-1111-1111-1111-111111111111' AS uuid), 1),
  ('Classes & Objects', 'LINK', 'Baeldung: OOP in Java',
     'https://www.baeldung.com/java-oop', CAST(NULL AS uuid), 0)
) AS m(lesson_title, type, title, url, file_id, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM materials x
  WHERE x.title = m.title
    AND x.lesson_id = (SELECT id FROM lessons WHERE title = m.lesson_title LIMIT 1));
