-- Seed the single faculty and its four departments.
-- Idempotent via ON CONFLICT on the unique columns.

INSERT INTO faculties (code, name)
VALUES ('FOE', 'Faculty of Engineering')
ON CONFLICT (code) DO NOTHING;

INSERT INTO departments (faculty_id, code, name)
SELECT f.id, d.code, d.name
FROM faculties f
JOIN (VALUES
    ('CE',  'Computer Engineering'),
    ('EE',  'Electrical Engineering'),
    ('ME',  'Mechanical Engineering'),
    ('CIV', 'Civil Engineering')
) AS d(code, name) ON TRUE
WHERE f.code = 'FOE'
ON CONFLICT (faculty_id, code) DO NOTHING;
