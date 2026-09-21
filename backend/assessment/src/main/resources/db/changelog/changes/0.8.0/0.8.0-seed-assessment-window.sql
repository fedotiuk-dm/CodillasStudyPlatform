-- Availability window relative to seeding time so "Take" works on the demo test.
UPDATE tests
SET available_from = now() - interval '1 day',
    available_until = now() + interval '60 days'
WHERE title = 'Java Control Test' AND available_until < now();
