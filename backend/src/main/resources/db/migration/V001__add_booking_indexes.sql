-- Migration: Add indexes for booking statistics drill-down feature
-- Purpose: Optimize queries filtering bookings by status and ordering by creation date
-- Date: 2026-05-08

-- Index for status filtering
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);

-- Index for creation date sorting
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(created_at DESC);

-- Composite index for filtered queries (status + creation date)
CREATE INDEX IF NOT EXISTS idx_bookings_status_created_at ON bookings(status, created_at DESC);

-- Note: These indexes will significantly improve performance for queries like:
-- SELECT * FROM bookings WHERE status IN ('pending', 'confirmed', 'in_progress') ORDER BY created_at DESC;
