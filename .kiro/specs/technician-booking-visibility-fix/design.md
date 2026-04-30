# Technician Booking Visibility Bugfix Design

## Overview

This design addresses a critical privacy and authorization bug where all technicians can see all pending bookings in the system, regardless of whether those bookings are assigned to them. The current implementation of `getPendingBookingsForTechnicians()` returns ALL pending bookings with no technician assignment filter, violating the principle of least privilege and exposing client information to unauthorized technicians.

The fix will modify the booking visibility logic to ensure technicians can ONLY see bookings that have been explicitly assigned to them (where `booking.technicianId == currentTechnician.id`). This requires changes to the service layer method signature, controller endpoint, and potentially the frontend to pass the authenticated technician's ID.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when a technician requests pending bookings without proper filtering by technicianId
- **Property (P)**: The desired behavior - technicians should only see bookings assigned to them (booking.technicianId == requestingTechnician.id)
- **Preservation**: Existing functionality that must remain unchanged - admin views, client views, technician's own bookings endpoint, booking acceptance flow
- **getPendingBookingsForTechnicians()**: The method in `BookingService.java` that currently returns all pending bookings without filtering by technician assignment
- **TechnicianBookingController**: The REST controller at `/api/v1/technician` that exposes the `/bookings/pending` endpoint
- **technicianId**: The UUID that identifies which technician a booking is assigned to (nullable field in Booking entity)

## Bug Details

### Bug Condition

The bug manifests when a technician calls the `/api/v1/technician/bookings/pending` endpoint to view available bookings. The system returns ALL pending bookings in the database, regardless of whether those bookings are assigned to the requesting technician. This occurs because the `getPendingBookingsForTechnicians()` method does not accept or use a technicianId parameter for filtering.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type HTTPRequest to /api/v1/technician/bookings/pending
  OUTPUT: boolean
  
  RETURN input.endpoint == "/api/v1/technician/bookings/pending"
         AND input.method == "GET"
         AND getPendingBookingsForTechnicians() is invoked
         AND NO technicianId filter is applied to the query
END FUNCTION
```

### Examples

- **Example 1**: Technician A (ID: `tech-aaa-111`) calls `/api/v1/technician/bookings/pending`. The system returns 10 pending bookings, including bookings assigned to Technician B (ID: `tech-bbb-222`). **Expected**: Only bookings where `technicianId == tech-aaa-111` should be returned.

- **Example 2**: Client creates a new booking that gets assigned to Technician C. When Technician D calls the pending bookings endpoint, they can see this booking including the client's service details. **Expected**: Technician D should NOT see this booking since it's not assigned to them.

- **Example 3**: A booking has status PENDING and technicianId is NULL (not yet assigned). All technicians can see this booking. **Expected**: Technicians should NOT see unassigned bookings (technicianId == NULL).

- **Edge Case**: Admin user calls admin endpoints to view all bookings - this should continue to work and show all bookings regardless of assignment. **Expected**: Admin functionality remains unchanged.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- The `/api/v1/technician/{technicianId}/bookings` endpoint must continue to return all bookings assigned to that specific technician across all statuses (PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, etc.)
- The booking acceptance flow (`/api/v1/technician/bookings/{bookingId}/accept`) must continue to work exactly as before
- Admin endpoints must continue to show all bookings regardless of technician assignment
- Client endpoints must continue to show all their bookings regardless of technician assignment
- The internal `getPendingBookings()` method (used for admin purposes) must continue to return all pending unassigned bookings
- Address privacy rules must continue to apply (address hidden until booking is confirmed and assigned)
- Notification system must continue to work when bookings are created, accepted, or status changes

**Scope:**
All requests that do NOT involve the `/api/v1/technician/bookings/pending` endpoint should be completely unaffected by this fix. This includes:
- Admin dashboard booking views
- Client booking views
- Technician's assigned bookings view (`/{technicianId}/bookings`)
- Booking acceptance and status update flows
- Internal service methods used by other parts of the system

## Hypothesized Root Cause

Based on the bug description and code analysis, the root causes are:

1. **Missing Parameter in Service Method**: The `getPendingBookingsForTechnicians()` method in `BookingService.java` does not accept a `technicianId` parameter, making it impossible to filter bookings by the requesting technician.

2. **Incorrect Query Logic**: The method calls `getPendingBookings()` which uses the repository method `findByStatusAndTechnicianIdIsNullOrderByCreatedAtAsc(BookingStatus.PENDING)`. This query specifically looks for bookings where `technicianId IS NULL`, which are unassigned bookings. The method should instead query for bookings where `technicianId == requestingTechnician.id`.

3. **Controller Not Passing Authentication Context**: The `TechnicianBookingController.getPendingBookings()` endpoint does not extract the authenticated technician's ID from the request and pass it to the service layer. The endpoint needs to be modified to accept and validate the technician's identity.

4. **Semantic Confusion**: The method name `getPendingBookingsForTechnicians()` (plural) suggests it returns bookings available to all technicians, when it should be `getPendingBookingsForTechnician()` (singular) to indicate it returns bookings for a specific technician.

## Correctness Properties

Property 1: Bug Condition - Technician Booking Visibility Filtering

_For any_ HTTP request to `/api/v1/technician/bookings/pending` where a technician is authenticated and requests their pending bookings, the fixed `getPendingBookingsForTechnicians()` method SHALL return ONLY bookings where `booking.technicianId == requestingTechnician.id` AND `booking.status == PENDING`, ensuring technicians can only see bookings explicitly assigned to them.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

Property 2: Preservation - Non-Pending Booking Endpoints

_For any_ request that is NOT to the `/api/v1/technician/bookings/pending` endpoint (such as admin views, client views, technician's full booking list, or booking acceptance flows), the fixed code SHALL produce exactly the same behavior as the original code, preserving all existing functionality for other booking-related operations.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `backend/src/main/java/com/G4/backend/service/BookingService.java`

**Method**: `getPendingBookingsForTechnicians()`

**Specific Changes**:

1. **Add technicianId Parameter**: Modify the method signature to accept a `UUID technicianId` parameter:
   ```java
   public List<Map<String, Object>> getPendingBookingsForTechnicians(UUID technicianId)
   ```

2. **Add Technician Validation**: Validate that the technician exists and has the correct role before querying bookings:
   ```java
   User technician = userRepository.findById(technicianId)
       .orElseThrow(() -> new BookingException("Technician not found", "TECHNICIAN_NOT_FOUND"));
   
   if (!"technician".equals(technician.getRole())) {
       throw new BookingException("User is not a technician", "INVALID_TECHNICIAN_ROLE");
   }
   ```

3. **Change Query Logic**: Replace the call to `getPendingBookings()` with a query that filters by technicianId:
   ```java
   // OLD: List<Booking> bookings = getPendingBookings();
   // NEW: Query for bookings assigned to this technician with PENDING status
   List<Booking> bookings = bookingRepository.findByTechnicianIdAndStatusOrderByCreatedAtAsc(
       technicianId, 
       BookingStatus.PENDING
   );
   ```

4. **Add Repository Method**: Add a new query method to `BookingRepository.java`:
   ```java
   List<Booking> findByTechnicianIdAndStatusOrderByCreatedAtAsc(UUID technicianId, BookingStatus status);
   ```

5. **Update Debug Logging**: Update the debug log message to reflect the filtered query:
   ```java
   System.out.println("DEBUG: Found " + bookings.size() + " pending bookings for technician " + technicianId);
   ```

**File**: `backend/src/main/java/com/G4/backend/controller/TechnicianBookingController.java`

**Method**: `getPendingBookings()`

**Specific Changes**:

1. **Add Request Parameter**: Modify the endpoint to accept technicianId as a request parameter or extract it from authentication context:
   ```java
   @GetMapping("/bookings/pending")
   public ResponseEntity<?> getPendingBookings(@RequestParam("technicianId") String technicianIdStr)
   ```

2. **Parse and Validate Input**: Convert the string parameter to UUID and handle parsing errors:
   ```java
   try {
       UUID technicianId = UUID.fromString(technicianIdStr);
       List<Map<String, Object>> bookings = bookingService.getPendingBookingsForTechnicians(technicianId);
       return ResponseEntity.ok(bookings);
   } catch (IllegalArgumentException e) {
       return ResponseEntity.badRequest().body(Map.of(
           "error", "Invalid technician ID format",
           "message", e.getMessage()
       ));
   }
   ```

3. **Alternative: Extract from JWT Token**: If the application uses JWT authentication, extract technicianId from the authenticated user context instead of requiring it as a parameter (more secure approach):
   ```java
   @GetMapping("/bookings/pending")
   public ResponseEntity<?> getPendingBookings(@AuthenticationPrincipal UserDetails userDetails) {
       // Extract technicianId from authenticated user
       UUID technicianId = extractTechnicianIdFromAuth(userDetails);
       // ... rest of implementation
   }
   ```

**File**: `backend/src/main/java/com/G4/backend/repository/BookingRepository.java`

**Specific Changes**:

1. **Add New Query Method**: Add a method to query bookings by technicianId and status:
   ```java
   List<Booking> findByTechnicianIdAndStatusOrderByCreatedAtAsc(UUID technicianId, BookingStatus status);
   ```

**Frontend Changes** (if applicable):

**File**: `web/src/pages/dashboard/Tdashboard.tsx` (or relevant component)

**Specific Changes**:

1. **Pass technicianId to API Call**: Modify the API call to include the authenticated technician's ID:
   ```typescript
   // OLD: const response = await fetch('/api/v1/technician/bookings/pending');
   // NEW: 
   const technicianId = getCurrentTechnicianId(); // Get from auth context
   const response = await fetch(`/api/v1/technician/bookings/pending?technicianId=${technicianId}`);
   ```

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Create test data with multiple technicians and bookings assigned to different technicians. Call the unfixed `/api/v1/technician/bookings/pending` endpoint as different technicians and observe that all technicians see all pending bookings. Run these tests on the UNFIXED code to observe failures and understand the root cause.

**Test Cases**:
1. **Cross-Technician Visibility Test**: Create bookings assigned to Technician A, then call the endpoint as Technician B. Observe that Technician B can see Technician A's bookings (will fail on unfixed code - demonstrates the bug).

2. **Unassigned Booking Visibility Test**: Create a booking with technicianId = NULL (unassigned). Call the endpoint as any technician and observe that they can see the unassigned booking (will fail on unfixed code - demonstrates the bug).

3. **Multiple Technician Test**: Create 5 bookings assigned to 3 different technicians. Call the endpoint as each technician and observe that each sees all 5 bookings instead of only their own (will fail on unfixed code - demonstrates the bug).

4. **No Bookings Test**: Call the endpoint as a technician who has no bookings assigned. Observe that they still see other technicians' bookings (will fail on unfixed code - demonstrates the bug).

**Expected Counterexamples**:
- Technicians can see bookings assigned to other technicians
- Technicians can see unassigned bookings (technicianId == NULL)
- No filtering is applied based on the requesting technician's identity
- Possible causes: missing technicianId parameter, incorrect query logic, no authentication context passed to service layer

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL request WHERE isBugCondition(request) DO
  result := getPendingBookingsForTechnicians_fixed(request.technicianId)
  ASSERT ALL booking IN result SATISFY booking.technicianId == request.technicianId
  ASSERT ALL booking IN result SATISFY booking.status == PENDING
END FOR
```

**Test Cases**:
1. **Technician A Isolation Test**: Create 3 bookings for Technician A and 3 bookings for Technician B. Call the fixed endpoint as Technician A. Assert that exactly 3 bookings are returned and all have technicianId == Technician A's ID.

2. **Technician B Isolation Test**: Using the same data, call the fixed endpoint as Technician B. Assert that exactly 3 bookings are returned and all have technicianId == Technician B's ID.

3. **Empty Result Test**: Call the fixed endpoint as a technician who has no pending bookings assigned. Assert that an empty list is returned (not other technicians' bookings).

4. **Status Filter Test**: Create bookings for Technician A with different statuses (PENDING, CONFIRMED, IN_PROGRESS). Call the fixed endpoint as Technician A. Assert that only PENDING bookings are returned.

5. **Invalid Technician Test**: Call the fixed endpoint with a non-existent technicianId. Assert that an appropriate error is returned (TECHNICIAN_NOT_FOUND).

6. **Non-Technician User Test**: Call the fixed endpoint with a clientId or adminId. Assert that an appropriate error is returned (INVALID_TECHNICIAN_ROLE).

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL request WHERE NOT isBugCondition(request) DO
  ASSERT originalEndpoint(request) = fixedEndpoint(request)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs

**Test Plan**: Observe behavior on UNFIXED code first for non-pending-bookings endpoints, then write property-based tests capturing that behavior.

**Test Cases**:

1. **Technician Full Bookings List Preservation**: Call `/api/v1/technician/{technicianId}/bookings` on unfixed code and record the response. Apply the fix and call the same endpoint. Assert that the responses are identical (this endpoint should show all statuses, not just PENDING).

2. **Admin Booking View Preservation**: Call admin endpoints to view all bookings on unfixed code. Apply the fix and call the same endpoints. Assert that admins still see all bookings regardless of technician assignment.

3. **Client Booking View Preservation**: Call client endpoints to view their bookings on unfixed code. Apply the fix and call the same endpoints. Assert that clients still see all their bookings regardless of technician assignment.

4. **Booking Acceptance Flow Preservation**: Test the booking acceptance flow (`/api/v1/technician/bookings/{bookingId}/accept`) on unfixed code. Apply the fix and test again. Assert that the acceptance flow works identically.

5. **Booking Status Update Preservation**: Test the status update endpoint (`/api/v1/technician/bookings/{bookingId}/status`) on unfixed code. Apply the fix and test again. Assert that status updates work identically.

6. **Address Privacy Preservation**: Verify that address information is still hidden for pending bookings and shown for confirmed bookings after the fix is applied.

7. **Internal getPendingBookings() Preservation**: If there are admin or internal uses of the `getPendingBookings()` method (which returns all unassigned pending bookings), verify that this method continues to work as before and is not affected by the fix.

### Unit Tests

- Test `getPendingBookingsForTechnicians(technicianId)` with valid technicianId returns only that technician's pending bookings
- Test `getPendingBookingsForTechnicians(technicianId)` with invalid technicianId throws TECHNICIAN_NOT_FOUND exception
- Test `getPendingBookingsForTechnicians(technicianId)` with non-technician user throws INVALID_TECHNICIAN_ROLE exception
- Test that bookings with status other than PENDING are not returned
- Test that bookings assigned to other technicians are not returned
- Test that the new repository method `findByTechnicianIdAndStatusOrderByCreatedAtAsc` correctly filters by both technicianId and status

### Property-Based Tests

- Generate random sets of bookings with different technicianIds and statuses, then verify that calling `getPendingBookingsForTechnicians(techId)` always returns only bookings where `booking.technicianId == techId` AND `booking.status == PENDING`
- Generate random technician IDs and verify that the method never returns bookings assigned to a different technician
- Generate random booking states and verify that non-pending bookings are never returned by this endpoint
- Test across many scenarios that other endpoints (admin, client, technician full list) continue to return the same results before and after the fix

### Integration Tests

- Test full flow: Create bookings for multiple technicians, authenticate as each technician, call the pending bookings endpoint, verify each sees only their own bookings
- Test that when a booking is accepted by a technician, it no longer appears in their pending bookings list (status changes from PENDING to CONFIRMED)
- Test that when a new booking is assigned to a technician, it appears in their pending bookings list
- Test that the frontend correctly passes the authenticated technician's ID to the backend and displays only their bookings
