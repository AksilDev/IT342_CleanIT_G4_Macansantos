# CleanIT Booking System Documentation - Updated Workflow

## Overview

The CleanIT booking system now follows the correct workflow where:
1. **Clients create bookings** without selecting a specific technician
2. **Technicians see pending bookings** and can accept them
3. **Address privacy** is enforced - technicians only see addresses after accepting
4. **Workload limits** prevent technicians from being overbooked
5. **Time slot conflicts** are automatically prevented

## Booking Status Workflow

### Status Definitions

1. **PENDING** - Booking created by client, awaiting technician acceptance
2. **CONFIRMED** - Technician has accepted the booking
3. **IN_PROGRESS** - Service is currently being performed
4. **COMPLETED** - Service has been completed successfully
5. **CANCELLED** - Booking has been cancelled
6. **NO_SHOW** - Client was not available at scheduled time

### Status Transition Rules

```
PENDING → CONFIRMED, CANCELLED
CONFIRMED → IN_PROGRESS, NO_SHOW, CANCELLED
IN_PROGRESS → COMPLETED
COMPLETED, CANCELLED, NO_SHOW → (Terminal states - no further transitions)
```

### User Permissions & Workflow

#### Client Journey
1. **Create Booking** - Select service, date, time, provide address
2. **Wait for Acceptance** - Booking shows as PENDING
3. **Get Notification** - When technician accepts (CONFIRMED)
4. **Track Progress** - IN_PROGRESS → COMPLETED
5. **Cancel if Needed** - Only while PENDING

#### Technician Journey
1. **View Pending Bookings** - See all available bookings (no addresses shown)
2. **Accept Booking** - System checks workload and conflicts
3. **See Full Details** - Address becomes visible after acceptance
4. **Update Status** - CONFIRMED → IN_PROGRESS → COMPLETED
5. **Handle No-Shows** - Mark as NO_SHOW if client unavailable

#### Admin Capabilities
- View all bookings
- Cancel any booking
- Override system restrictions (if needed)

## Key Business Rules

### Workload Management
- **Maximum Active Bookings**: 1 CONFIRMED + 1 IN_PROGRESS per technician
- **Automatic Enforcement**: System prevents acceptance if limit exceeded
- **Real-time Validation**: Checked at booking acceptance time

### Time Slot Conflicts
- **Conflict Prevention**: Technicians cannot accept overlapping time slots
- **Automatic Checking**: System validates before allowing acceptance
- **Status Consideration**: Only CONFIRMED and IN_PROGRESS bookings block slots

### Address Privacy
- **Pending Bookings**: Address hidden from all technicians
- **After Acceptance**: Address visible only to assigned technician
- **Client Access**: Clients always see their own addresses
- **Security**: Prevents address harvesting by unassigned technicians

## API Endpoints

### Client Endpoints

#### Create Booking
```http
POST /api/v1/bookings/create
Content-Type: application/json

{
  "clientId": "uuid",
  "serviceType": "Deep Cleaning",
  "deviceType": "Laptop",
  "addOns": ["Thermal Paste"],
  "timeSlot": "10:00 AM - 12:00 PM",
  "bookingDate": "2024-12-25",
  "address": "123 Main St, City",
  "landmark": "Near the mall",
  "specialInstructions": "Handle with care",
  "totalAmount": 150.00,
  "estimatedDuration": 2.0
}
```

#### Get Client Bookings
```http
GET /api/v1/bookings/client/{clientId}
```

#### Cancel Booking
```http
POST /api/v1/bookings/{bookingId}/cancel
Content-Type: application/json

{
  "userId": "uuid",
  "reason": "Schedule conflict"
}
```

### Technician Endpoints

#### Get Pending Bookings
```http
GET /api/v1/technician/bookings/pending
```

#### Accept Booking
```http
POST /api/v1/technician/bookings/{bookingId}/accept
Content-Type: application/json

{
  "technicianId": "uuid"
}
```

#### Get Technician's Bookings
```http
GET /api/v1/technician/{technicianId}/bookings
```

#### Toggle Availability
```http
POST /api/v1/technician/{technicianId}/availability
Content-Type: application/json

{
  "isAvailable": true
}
```

#### Get Availability Status
```http
GET /api/v1/technician/{technicianId}/availability
```

## Database Schema Updates

### Booking Entity Changes

| Field | Type | Description | Changes |
|-------|------|-------------|---------|
| technicianId | UUID | Reference to technician | **Now nullable** - assigned on acceptance |
| bookingCode | String | Unique reference (BK-2024-00123) | **New field** |
| startedAt | LocalDateTime | Service start timestamp | **New field** |
| cancelledAt | LocalDateTime | Cancellation timestamp | **New field** |
| noShowAt | LocalDateTime | No-show timestamp | **New field** |

### New TechnicianSettings Entity

| Field | Type | Description |
|-------|------|-------------|
| technicianId | UUID | Primary key, references User |
| isAvailable | Boolean | Availability toggle |
| updatedAt | LocalDateTime | Last update timestamp |

## Validation & Error Handling

### Booking Creation Validation
- ✅ Client must exist and be verified
- ✅ Client role must be "client"
- ✅ All required fields provided
- ✅ Future booking date

### Booking Acceptance Validation
- ✅ Booking must be PENDING
- ✅ No technician already assigned
- ✅ Technician must exist and be verified
- ✅ Technician role must be "technician"
- ✅ Technician must be available
- ✅ Workload limit not exceeded (max 2 active bookings)
- ✅ No time slot conflicts

### Error Codes
- `BOOKING_NOT_PENDING` - Booking already accepted/completed
- `BOOKING_ALREADY_ASSIGNED` - Another technician already accepted
- `WORKLOAD_LIMIT_EXCEEDED` - Technician has maximum bookings
- `TIME_SLOT_CONFLICT` - Technician busy at that time
- `TECHNICIAN_UNAVAILABLE` - Technician marked as unavailable

## Notification System

### Automatic Notifications
- **New Booking Created** → Notify all available technicians
- **Booking Accepted** → Notify client
- **Status Changes** → Notify relevant parties
- **Cancellations** → Notify all involved parties

## Security Features

### Address Privacy Enforcement
```java
// Address only shown to:
// 1. Client (always)
// 2. Assigned technician (after acceptance)
// 3. Admin (always)

if (includeAddress) {
    map.put("address", booking.getAddress());
} else {
    map.put("address", "Address will be visible after booking confirmation");
}
```

### Role-Based Access Control
- **Clients**: Create, view own bookings, cancel pending bookings
- **Technicians**: View pending bookings, accept bookings, update status
- **Admins**: Full access to all operations

## Usage Examples

### Complete Booking Flow

1. **Client Creates Booking**
```bash
curl -X POST /api/v1/bookings/create \
  -H "Content-Type: application/json" \
  -d '{"clientId":"uuid","serviceType":"Deep Cleaning",...}'
```

2. **Technician Views Pending Bookings**
```bash
curl /api/v1/technician/bookings/pending
# Returns bookings without addresses
```

3. **Technician Accepts Booking**
```bash
curl -X POST /api/v1/technician/bookings/{bookingId}/accept \
  -H "Content-Type: application/json" \
  -d '{"technicianId":"uuid"}'
```

4. **Technician Views Accepted Bookings**
```bash
curl /api/v1/technician/{technicianId}/bookings
# Now shows full address details
```

## Key Improvements Made

✅ **Correct Workflow**: Bookings created without technician assignment
✅ **Technician Dashboard**: Shows pending bookings for acceptance
✅ **Address Privacy**: Protected until booking acceptance
✅ **Workload Management**: Automatic enforcement of booking limits
✅ **Conflict Prevention**: Time slot validation
✅ **Availability Toggle**: Technicians can mark themselves unavailable
✅ **Proper Status Flow**: Simplified, logical status transitions
✅ **Role-Based Security**: Appropriate permissions for each user type

This implementation now correctly matches your specification where technicians see pending bookings on their dashboard and can accept them, with proper privacy and workload controls in place.