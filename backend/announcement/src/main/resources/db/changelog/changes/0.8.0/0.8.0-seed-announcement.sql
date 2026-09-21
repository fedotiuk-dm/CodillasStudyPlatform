-- Roster read model (normally fed by StudentEnrolled) for the seeded enrolment.
INSERT INTO announcement_memberships (id, group_id, student_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Sam Student' LIMIT 1),
       '2026-03-02 09:00:00+00', '2026-03-02 09:00:00+00'
ON CONFLICT DO NOTHING;

INSERT INTO announcements (id, group_id, author_id, title, body, pinned, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id FROM study_groups WHERE name = 'Java Cohort A' LIMIT 1),
       (SELECT user_id FROM user_profiles WHERE display_name = 'Tom Teacher' LIMIT 1),
       'Welcome to Java Cohort A',
       'Lessons run on Wednesdays at 17:00. Homework is due the following Tuesday.',
       true,
       '2026-03-02 09:10:00+00', '2026-03-02 09:10:00+00'
WHERE NOT EXISTS (SELECT 1 FROM announcements WHERE title = 'Welcome to Java Cohort A');
