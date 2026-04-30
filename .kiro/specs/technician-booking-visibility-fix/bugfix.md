# Bugfix Requirements Document

## Introduction

This document addresses a critical privacy and usability bug in the technician booking visibility system. Currently, ALL technicians can see ALL pending bookings in the system, regardless of whether those bookings have been assigned to them. This violates privacy expectations and creates confusion for technicians who see bookings they cannot or should not accept.

The correct behavior should ensure that technicians can ONLY see bookings that have been explicitly assigned to them (where `booking.technicianId == currentTechnician.id`). Pending bookings that have not yet been assigned to any technician should NOT be visible to technicians.

**Impact**: This bug affects all technicians using the system and compromises client privacy by exposing booking details to unauthorized technicians.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a technician calls the `/api/v1/technician/bookings/pending` endpoint THEN the system returns ALL pending bookings in the system regardless of technician assignment

1.2 WHEN Client1 creates a booking and it gets assigned to Technician1 THEN Technician2 can also see this booking in their pending bookings list

1.3 WHEN the `getPendingBookingsForTechnicians()` method is invoked THEN it calls `getPendingBookings()` which returns ALL bookings with status PENDING and no technician assigned

1.4 WHEN a booking has status PENDING and technicianId is NULL THEN the booking is visible to ALL technicians through the pending bookings endpoint

### Expected Behavior (Correct)

2.1 WHEN a technician calls the `/api/v1/technician/bookings/pending` endpoint THEN the system SHALL return ONLY bookings where `booking.technicianId == currentTechnician.id` AND status is PENDING

2.2 WHEN Client1 creates a booking and it gets assigned to Technician1 THEN ONLY Technician1 SHALL see this booking in their pending bookings list

2.3 WHEN the `getPendingBookingsForTechnicians()` method is invoked with a technicianId parameter THEN it SHALL filter bookings by the specified technicianId

2.4 WHEN a booking has status PENDING and technicianId is NULL (not yet assigned) THEN the booking SHALL NOT be visible to any technician through the pending bookings endpoint

2.5 WHEN a technician requests pending bookings THEN the system SHALL validate that the requesting user is a verified technician before returning any data

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a technician calls `/api/v1/technician/{technicianId}/bookings` THEN the system SHALL CONTINUE TO return all bookings assigned to that technician across all statuses

3.2 WHEN a technician accepts a pending booking through `/api/v1/technician/bookings/{bookingId}/accept` THEN the system SHALL CONTINUE TO assign the technician and change status to CONFIRMED

3.3 WHEN a booking transitions from PENDING to CONFIRMED THEN the system SHALL CONTINUE TO send notifications to the client and assigned technician

3.4 WHEN an admin views bookings THEN the system SHALL CONTINUE TO show all bookings regardless of assignment

3.5 WHEN a client views their bookings THEN the system SHALL CONTINUE TO show all their bookings regardless of technician assignment

3.6 WHEN the `getPendingBookings()` method is called internally for admin purposes THEN it SHALL CONTINUE TO return all pending unassigned bookings

3.7 WHEN address information is requested for a booking THEN the system SHALL CONTINUE TO hide address details until the booking is confirmed and assigned to a technician
