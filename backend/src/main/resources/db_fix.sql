-- Fix technician_id constraint to allow NULL values for PENDING bookings
ALTER TABLE bookings ALTER COLUMN technician_id DROP NOT NULL;

-- Verify the change
\d bookings;