-- Migration: 2026-05-10-seed-courses-and-batch.sql
-- Seeds 3 placeholder courses and ensures a 2026 batch exists.
-- Safe to re-run (INSERT IGNORE).

INSERT IGNORE INTO courses (course_code, course_name) VALUES
  ('CARS',  'Culinary Arts and Restaurant Services'),
  ('BPRO',  'Bread and Pastry Production'),
  ('FSERV', 'Food and Beverage Services');

INSERT IGNORE INTO batches (batch_code, batch_year) VALUES
  ('B2026A', 2026);
