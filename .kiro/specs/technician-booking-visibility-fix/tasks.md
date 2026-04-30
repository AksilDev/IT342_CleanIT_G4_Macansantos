# Implementation Plan

## Overview
This task list implements the technician booking visibility bugfix using the bug condition methodology. The fix ensures technicians can ONLY see bookings explicitly assigned to them (where `booking.technicianId == currentTechnician.id`), preventing unauthorized access to other technicians' bookings and unassigned bookings.

## Task Breakdown

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Technician Cross-Visibility Bug
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate unauthorized booking visibility
  - **Scoped PBT Approach**: Test with multiple technicians and verify each can only see their own assigned pending bookings
  - Test implementation details from Bug Condition in design:
    - Create test data: 3 technicians (Tech A, Tech B, Tech C)
    - Create 6 pending bookings: 2 assigned to Tech A, 2 assigned to Tech B, 2 unassigned (technicianId = NULL)
    - Call `/api/v1/technician/bookings/pending` as Tech A
    - Assert that ONLY 2 bookings are returned (those assigned to Tech A)
    - Assert that ALL returned bookings have `technicianId == Tech A's ID`
    - Assert that bookings assigned to Tech B are NOT visible
    - Assert that unassigned bookings (technicianId = NULL) are NOT visible
  - The test assertions should match the Expected Behavior Properties from design:
    - For all bookings in result: `booking.technicianId == requestingTechnician.id`
    - For all bookings in result: `booking.status == PENDING`
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found:
    - "Tech A can see bookings assigned to Tech B"
    - "Tech A can see unassigned bookings (technicianId = NULL)"
    - "All technicians see all pending bookings regardless of assignment"
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Pending Booking Endpoints Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs (endpoints other than `/api/v1/technician/bookings/pending`)
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements:
    - Test 1: Technician Full Bookings List (`/api/v1/technician/{technicianId}/bookings`)
      - Create bookings for Tech A with statuses: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED
      - Call endpoint as Tech A on UNFIXED code
      - Observe: Returns all bookings across all statuses for Tech A
      - Write property: For all statuses, endpoint returns bookings where `technicianId == Tech A's ID`
    - Test 2: Booking Acceptance Flow (`/api/v1/technician/bookings/{bookingId}/accept`)
      - Create unassigned pending booking on UNFIXED code
      - Call accept endpoint as Tech A
      - Observe: Booking gets assigned to Tech A and status changes to CONFIRMED
      - Write property: Acceptance flow assigns technician and confirms booking
    - Test 3: Admin Booking View (if admin endpoints exist)
      - Call admin booking endpoints on UNFIXED code
      - Observe: Admin sees all bookings regardless of technician assignment
      - Write property: Admin views return all bookings without filtering
    - Test 4: Client Booking View
      - Create bookings for Client X on UNFIXED code
      - Call client booking endpoint
      - Observe: Client sees all their bookings regardless of technician assignment
      - Write property: Client views return all bookings for that client
  - Property-based testing generates many test cases for stronger guarantees
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 3. Fix for technician booking visibility bug

  - [x] 3.1 Add new repository query method
    - Open `backend/src/main/java/com/G4/backend/repository/BookingRepository.java`
    - Add new query method to filter by technicianId and status:
      ```java
      List<Booking> findByTechnicianIdAndStatusOrderByCreatedAtAsc(UUID technicianId, BookingStatus status);
      ```
    - This method will be used by the service layer to query only bookings assigned to a specific technician with PENDING status
    - _Bug_Condition: isBugCondition(input) where input.endpoint == "/api/v1/technician/bookings/pending" AND NO technicianId filter is applied_
    - _Expected_Behavior: For all requests to pending bookings endpoint, return ONLY bookings where booking.technicianId == requestingTechnician.id AND booking.status == PENDING_
    - _Preservation: Other repository methods remain unchanged_
    - _Requirements: 2.1, 2.3_

  - [x] 3.2 Modify BookingService.getPendingBookingsForTechnicians() method
    - Open `backend/src/main/java/com/G4/backend/service/BookingService.java`
    - Locate the `getPendingBookingsForTechnicians()` method (around line 220)
    - Add `UUID technicianId` parameter to method signature:
      ```java
      public List<Map<String, Object>> getPendingBookingsForTechnicians(UUID technicianId)
      ```
    - Add technician validation at the start of the method:
      ```java
      // Validate technician exists and has correct role
      User technician = userRepository.findById(technicianId)
          .orElseThrow(() -> new BookingException("Technician not found", "TECHNICIAN_NOT_FOUND"));
      
      if (!"technician".equals(technician.getRole())) {
          throw new BookingException("User is not a technician", "INVALID_TECHNICIAN_ROLE");
      }
      ```
    - Replace the query logic:
      ```java
      // OLD: List<Booking> bookings = getPendingBookings();
      // NEW: Query for bookings assigned to this technician with PENDING status
      List<Booking> bookings = bookingRepository.findByTechnicianIdAndStatusOrderByCreatedAtAsc(
          technicianId, 
          BookingStatus.PENDING
      );
      ```
    - Update debug logging:
      ```java
      System.out.println("DEBUG: Found " + bookings.size() + " pending bookings for technician " + technicianId);
      ```
    - Keep the rest of the method unchanged (response mapping logic)
    - _Bug_Condition: isBugCondition(input) from design - no technicianId filter applied_
    - _Expected_Behavior: expectedBehavior(result) from design - only bookings assigned to requesting technician_
    - _Preservation: getPendingBookings() method remains unchanged for admin use, other service methods unaffected_
    - _Requirements: 2.1, 2.2, 2.3, 2.5_

  - [x] 3.3 Update TechnicianBookingController.getPendingBookings() endpoint
    - Open `backend/src/main/java/com/G4/backend/controller/TechnicianBookingController.java`
    - Locate the `getPendingBookings()` method (around line 28)
    - Add `@RequestParam` to accept technicianId from query parameter:
      ```java
      @GetMapping("/bookings/pending")
      public ResponseEntity<?> getPendingBookings(@RequestParam("technicianId") String technicianIdStr)
      ```
    - Add parameter parsing and validation:
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
      } catch (Exception e) {
          return ResponseEntity.badRequest().body(Map.of(
              "error", "Failed to fetch pending bookings",
              "message", e.getMessage()
          ));
      }
      ```
    - Alternative approach (if JWT authentication is available): Extract technicianId from authenticated user context instead of query parameter for better security
    - _Bug_Condition: isBugCondition(input) from design - endpoint does not pass technicianId to service layer_
    - _Expected_Behavior: expectedBehavior(result) from design - endpoint passes authenticated technician's ID to service layer_
    - _Preservation: Other controller endpoints remain unchanged_
    - _Requirements: 2.1, 2.5_

  - [x] 3.4 Update frontend API call (if applicable)
    - Open `web/src/pages/dashboard/Tdashboard.tsx`
    - Locate the API call to `/api/v1/technician/bookings/pending`
    - Modify the fetch call to include technicianId query parameter:
      ```typescript
      // Get authenticated technician's ID from auth context
      const technicianId = getCurrentTechnicianId(); // Or from auth state/context
      
      // Update API call
      const response = await fetch(`/api/v1/technician/bookings/pending?technicianId=${technicianId}`);
      ```
    - Ensure error handling is in place for invalid technicianId or authorization errors
    - _Bug_Condition: Frontend does not pass technicianId to backend_
    - _Expected_Behavior: Frontend passes authenticated technician's ID to backend_
    - _Preservation: Other frontend API calls remain unchanged_
    - _Requirements: 2.1_

  - [x] 3.5 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Technician Booking Isolation
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - Verify that:
      - Tech A sees ONLY their 2 assigned pending bookings
      - Tech A does NOT see Tech B's bookings
      - Tech A does NOT see unassigned bookings (technicianId = NULL)
      - All returned bookings have `technicianId == Tech A's ID`
      - All returned bookings have `status == PENDING`
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: Expected Behavior Properties from design - 2.1, 2.2, 2.3, 2.4_

  - [x] 3.6 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Pending Booking Endpoints Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - Verify that:
      - Technician Full Bookings List endpoint still returns all statuses for assigned technician
      - Booking acceptance flow still works correctly
      - Admin views still show all bookings
      - Client views still show all their bookings
      - Address privacy rules still apply correctly
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: Preservation Requirements from design - 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 4. Checkpoint - Ensure all tests pass
  - Run all exploration tests (task 1) - should now PASS
  - Run all preservation tests (task 2) - should still PASS
  - Run any existing unit tests for BookingService and TechnicianBookingController
  - Run integration tests if available
  - Verify no compilation errors
  - Verify no breaking changes to other endpoints
  - Ask the user if questions arise or if additional testing is needed

## Notes

- **Bug Condition**: The bug occurs when technicians call `/api/v1/technician/bookings/pending` and receive ALL pending bookings instead of only their assigned bookings
- **Expected Behavior**: Technicians should ONLY see bookings where `booking.technicianId == currentTechnician.id` AND `status == PENDING`
- **Preservation**: All other booking endpoints (admin, client, technician full list, acceptance flow) must remain unchanged
- **Testing Strategy**: Write tests BEFORE implementing the fix to confirm the bug exists, then verify the fix resolves the bug without breaking existing functionality
