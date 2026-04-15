# CleanIT API Endpoints Documentation

## Overview
Complete API documentation for the CleanIT booking system with technician acceptance workflow and admin dashboard.

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
All endpoints require proper authentication. Include JWT token in Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## 📋 Booking Endpoints

### Create Booking
**POST** `/bookings/create`

Creates a new booking without technician assignment.

**Request Body:**
```json
{
  "clientId": "uuid",
  "serviceType": "Deep Cleaning",
  "deviceType": "Laptop",
  "addOns": ["Thermal Paste", "Cable Management"],
  "timeSlot": "10:00 AM - 12:00 PM",
  "bookingDate": "2024-12-25",
  "address": "123 Main Street, City",
  "landmark": "Near the shopping mall",
  "specialInstructions": "Handle with care",
  "totalAmount": 150.00,
  "estimatedDuration": 2.0
}
```

**Response:**
```json
{
  "id": "uuid",
  "bookingCode": "BK-2024-00123",
  "serviceType": "Deep Cleaning",
  "deviceType": "Laptop",
  "timeSlot": "10:00 AM - 12:00 PM",
  "bookingDate": "2024-12-25",
  "totalAmount": 150.00,
  "status": "pending",
  "statusDescription": "Booking created and awaiting technician acceptance",
  "createdAt": "2024-12-20T10:30:00"
}
```

### Get Client Bookings
**GET** `/bookings/client/{clientId}`

Returns all bookings for a specific client with acceptance status.

**Response:**
```json
[
  {
    "id": "uuid",
    "bookingCode": "BK-2024-00123",
    "serviceType": "Deep Cleaning",
    "status": "confirmed",
    "technicianAssigned": true,
    "technicianAcceptedAt": "2024-12-20T11:00:00",
    "acceptanceMessage": "Your booking has been accepted by a technician!",
    "statusMessage": "Great! A technician has accepted your booking.",
    "address": "123 Main Street, City",
    "totalAmount": 150.00,
    "bookingDate": "2024-12-25",
    "createdAt": "2024-12-20T10:30:00"
  }
]
```

### Get Booking Details
**GET** `/bookings/{bookingId}?requestingUserId={userId}`

Returns detailed booking information with appropriate privacy controls.

### Cancel Booking
**POST** `/bookings/{bookingId}/cancel`

**Request Body:**
```json
{
  "userId": "uuid",
  "reason": "Schedule conflict"
}
```

---

## 🔧 Technician Endpoints

### Get Pending Bookings
**GET** `/technician/bookings/pending`

Returns all pending bookings that technicians can accept (addresses hidden).

**Response:**
```json
[
  {
    "id": "uuid",
    "bookingCode": "BK-2024-00123",
    "serviceType": "Deep Cleaning",
    "deviceType": "Laptop",
    "timeSlot": "10:00 AM - 12:00 PM",
    "bookingDate": "2024-12-25",
    "totalAmount": 150.00,
    "address": "Address will be visible after booking confirmation",
    "clientName": "John Doe",
    "specialInstructions": "Handle with care",
    "estimatedDuration": 2.0,
    "createdAt": "2024-12-20T10:30:00"
  }
]
```

### Accept Booking
**POST** `/technician/bookings/{bookingId}/accept`

Technician accepts a pending booking with automatic validation.

**Request Body:**
```json
{
  "technicianId": "uuid"
}
```

**Response:**
```json
{
  "message": "Booking accepted successfully",
  "booking": {
    "id": "uuid",
    "status": "confirmed",
    "technicianId": "uuid",
    "confirmedAt": "2024-12-20T11:00:00"
  }
}
```

**Error Responses:**
- `WORKLOAD_LIMIT_EXCEEDED` - Technician has maximum bookings (2)
- `TIME_SLOT_CONFLICT` - Technician busy at that time
- `BOOKING_ALREADY_ASSIGNED` - Another technician accepted first
- `TECHNICIAN_UNAVAILABLE` - Technician marked as unavailable

### Get Technician Bookings
**GET** `/technician/{technicianId}/bookings`

Returns all bookings assigned to a technician (with full address details).

### Update Booking Status
**POST** `/technician/bookings/{bookingId}/status`

Technician updates booking progress.

**Request Body:**
```json
{
  "technicianId": "uuid",
  "status": "in_progress",
  "reason": "Service started"
}
```

**Valid Status Transitions:**
- `confirmed` → `in_progress`
- `in_progress` → `completed`
- `confirmed` → `no_show`

### Toggle Availability
**POST** `/technician/{technicianId}/availability`

**Request Body:**
```json
{
  "isAvailable": true
}
```

### Get Availability Status
**GET** `/technician/{technicianId}/availability`

**Response:**
```json
{
  "isAvailable": true
}
```

### Get Technician Statistics
**GET** `/technician/{technicianId}/statistics`

**Response:**
```json
{
  "totalBookings": 25,
  "completed": 20,
  "confirmed": 2,
  "inProgress": 1,
  "active": 3,
  "totalEarnings": 3750.00
}
```

---

## 👨‍💼 Admin Dashboard Endpoints

### Get Dashboard Statistics
**GET** `/admin/dashboard/statistics`

Returns comprehensive booking statistics for admin dashboard.

**Response:**
```json
{
  "total": 150,
  "pending": 5,
  "confirmed": 8,
  "in_progress": 3,
  "completed": 120,
  "cancelled": 12,
  "no_show": 2,
  "today": 4,
  "thisWeek": 18,
  "thisMonth": 67,
  "totalRevenue": 18750.00,
  "monthRevenue": 8250.00
}
```

### Get Dashboard Overview
**GET** `/admin/dashboard/overview`

Returns complete dashboard data with statistics, recent bookings, and metrics.

**Response:**
```json
{
  "statistics": {
    "total": 150,
    "pending": 5,
    "confirmed": 8,
    "completed": 120,
    "totalRevenue": 18750.00
  },
  "recentBookings": [
    {
      "id": "uuid",
      "bookingCode": "BK-2024-00123",
      "serviceType": "Deep Cleaning",
      "status": "confirmed",
      "clientName": "John Doe",
      "technicianName": "Jane Smith",
      "totalAmount": 150.00,
      "createdAt": "2024-12-20T10:30:00"
    }
  ],
  "metrics": {
    "completionRate": 80,
    "activeBookings": 13,
    "totalRevenue": 18750.00,
    "monthRevenue": 8250.00
  }
}
```

### Get Recent Bookings
**GET** `/admin/dashboard/recent-bookings?limit=10`

Returns recent bookings for admin dashboard.

### Get All Bookings (Paginated)
**GET** `/admin/bookings?page=0&size=20`

Returns paginated list of all bookings for admin management.

**Response:**
```json
{
  "bookings": [...],
  "page": 0,
  "size": 20,
  "hasMore": true
}
```

### Get Bookings by Status
**GET** `/admin/bookings/status/{status}`

Returns all bookings with specific status.

**Valid Status Values:**
- `pending`
- `confirmed`
- `in_progress`
- `completed`
- `cancelled`
- `no_show`

---

## 🔄 Workflow Examples

### Complete Booking Flow

1. **Client Creates Booking**
```bash
POST /api/v1/bookings/create
# Status: PENDING, no technician assigned
```

2. **Technicians See Pending Booking**
```bash
GET /api/v1/technician/bookings/pending
# Shows booking without address
```

3. **Technician Accepts Booking**
```bash
POST /api/v1/technician/bookings/{id}/accept
# Status: PENDING → CONFIRMED
# Address becomes visible to technician
# Client gets notification
```

4. **Client Sees Acceptance**
```bash
GET /api/v1/bookings/client/{clientId}
# Shows "technicianAssigned": true
# Shows acceptance message and timestamp
```

5. **Technician Updates Progress**
```bash
POST /api/v1/technician/bookings/{id}/status
# CONFIRMED → IN_PROGRESS → COMPLETED
```

6. **Admin Monitors Everything**
```bash
GET /api/v1/admin/dashboard/overview
# Real-time statistics and metrics
```

---

## 📊 Admin Dashboard Metrics

### Key Statistics Displayed:
- **Total Bookings**: All-time booking count
- **Status Breakdown**: Count by each status
- **Time-based Metrics**: Today, this week, this month
- **Revenue Tracking**: Total and monthly revenue
- **Completion Rate**: Percentage of completed bookings
- **Active Bookings**: Currently pending + confirmed + in-progress

### Real-time Updates:
- Booking counts update when status changes
- Revenue calculations update on completion
- Recent bookings list shows latest activity

---

## 🔒 Security & Privacy

### Address Privacy Rules:
- **Pending Bookings**: Address hidden from all technicians
- **After Acceptance**: Address visible only to assigned technician
- **Client Access**: Always see their own addresses
- **Admin Access**: Can see all addresses

### Role-based Permissions:
- **Clients**: Create, view own bookings, cancel pending
- **Technicians**: View pending, accept, update assigned bookings
- **Admins**: Full access to all operations and statistics

### Validation Rules:
- **Workload Limits**: Max 2 active bookings per technician
- **Time Conflicts**: Prevent overlapping bookings
- **Status Transitions**: Only valid state changes allowed
- **Availability**: Respect technician availability settings

This API provides a complete booking management system with proper workflow, privacy controls, and comprehensive admin oversight.