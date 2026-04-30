# Implementation Plan

## Overview

This task list implements the pre-service checklist workflow fix using the bug condition methodology. The workflow follows the exploratory bugfix approach:
1. **Explore** - Write tests BEFORE fix to understand the bug (Bug Condition)
2. **Preserve** - Write tests for non-buggy behavior (Preservation Requirements)
3. **Implement** - Apply the fix with understanding (Expected Behavior)
4. **Validate** - Verify fix works and doesn't break anything

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Checklist Initialization and Validation Timing
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Test concrete scenarios: (1) checklist initialized at booking creation, (2) validation at CONFIRMED→IN_PROGRESS, (3) no validation at IN_PROGRESS→COMPLETED, (4) 12 items instead of 5
  - Test that checklist is initialized at booking creation (should be true on unfixed code, false on fixed code)
  - Test that checklist validation occurs at CONFIRMED → IN_PROGRESS transition (should be true on unfixed code, false on fixed code)
  - Test that checklist validation does NOT occur at IN_PROGRESS → COMPLETED transition (should be true on unfixed code, false on fixed code)
  - Test that checklist contains 12 items instead of 5 (should be true on unfixed code, false on fixed code)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found to understand root cause
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Checklist Operations Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-checklist operations:
    - Booking creation (client validation, add-on compatibility)
    - Technician acceptance (PENDING → CONFIRMED with availability, workload, time slot checks)
    - Photo validation at IN_PROGRESS → COMPLETED transition
    - Status transitions to CANCELLED and NO_SHOW
    - Checklist item toggling (technician assignment verification)
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements
  - Property-based testing generates many test cases for stronger guarantees
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 3. Fix checklist workflow timing

  - [x] 3.1 Update DataInitializer.java - Replace 12-item checklist with 5 pre-service items
    - Open `backend/src/main/java/com/G4/backend/config/DataInitializer.java`
    - Locate the checklist initialization section (lines 115-128)
    - Replace the 12-item checklist with 5 pre-service items:
      - "Verify location is valid and searchable"
      - "Inspect tools for service are clean and working"
      - "Client available and gives consent"
      - "Test device is working before beginning physical service"
      - "Review service requirements with client"
    - _Bug_Condition: checklistItemCount() == 12 AND NOT checklistItemCount() == 5_
    - _Expected_Behavior: Checklist SHALL contain exactly 5 pre-service items with correct labels (Requirement 2.7)_
    - _Preservation: Existing service and add-on initialization must remain unchanged_
    - _Requirements: 2.7_

  - [x] 3.2 Update BookingService.java - Remove early checklist initialization
    - Open `backend/src/main/java/com/G4/backend/service/BookingService.java`
    - Locate the `createBooking` method (around line 130)
    - Remove the call to `initializeBookingChecklist(savedBooking)`
    - Add comment: `// Checklist will be initialized when service starts (CONFIRMED -> IN_PROGRESS)`
    - _Bug_Condition: input.operation == "createBooking" AND checklistInitializedImmediately()_
    - _Expected_Behavior: System SHALL NOT initialize any checklist at booking creation (Requirement 2.1)_
    - _Preservation: All other booking creation logic (client validation, add-on compatibility, notifications) must remain unchanged_
    - _Requirements: 2.1_

  - [x] 3.3 Update BookingService.java - Add checklist initialization at service start
    - In the `updateBookingStatus` method (around line 340)
    - Add checklist initialization logic BEFORE the existing validation block:
    ```java
    // AC-11 FIX: Initialize checklist when starting service (CONFIRMED -> IN_PROGRESS)
    if (oldStatus == BookingStatus.CONFIRMED && newStatus == BookingStatus.IN_PROGRESS) {
        initializeBookingChecklist(booking);
    }
    ```
    - _Bug_Condition: input.operation == "updateStatus" AND input.oldStatus == CONFIRMED AND input.newStatus == IN_PROGRESS AND NOT checklistInitialized()_
    - _Expected_Behavior: System SHALL initialize a 5-item pre-service checklist when transitioning CONFIRMED → IN_PROGRESS (Requirement 2.2)_
    - _Preservation: All other status transition logic must remain unchanged_
    - _Requirements: 2.2_

  - [x] 3.4 Update BookingService.java - Remove incorrect validation at service start
    - In the `updateBookingStatus` method (around lines 350-361)
    - Remove the entire checklist validation block for CONFIRMED → IN_PROGRESS transition
    - This validation is incorrect because the checklist is being created at this point
    - _Bug_Condition: input.operation == "updateStatus" AND input.oldStatus == CONFIRMED AND input.newStatus == IN_PROGRESS AND checklistValidationPerformed()_
    - _Expected_Behavior: System SHALL NOT validate checklist completion at CONFIRMED → IN_PROGRESS transition (Requirement 2.6)_
    - _Preservation: Photo validation and other status transition validations must remain unchanged_
    - _Requirements: 2.6_

  - [x] 3.5 Update BookingService.java - Add validation at service completion
    - In the `updateBookingStatus` method (around line 363)
    - Add checklist validation BEFORE the existing photo validation:
    ```java
    // AC-11 FIX: Validate checklist completion before completing service (IN_PROGRESS -> COMPLETED)
    if (oldStatus == BookingStatus.IN_PROGRESS && newStatus == BookingStatus.COMPLETED) {
        Map<String, Object> checklistValidation = validateChecklistComplete(bookingId);
        if (!(Boolean) checklistValidation.get("isComplete")) {
            @SuppressWarnings("unchecked")
            List<String> incompleteItems = (List<String>) checklistValidation.get("incompleteItems");
            throw new BookingException(
                "All pre-service checklist items must be completed before finishing service. Missing: " + 
                String.join(", ", incompleteItems),
                "CHECKLIST_INCOMPLETE"
            );
        }
    }
    ```
    - _Bug_Condition: input.operation == "updateStatus" AND input.oldStatus == IN_PROGRESS AND input.newStatus == COMPLETED AND NOT checklistValidationPerformed()_
    - _Expected_Behavior: System SHALL validate that all 5 pre-service checklist items are completed at IN_PROGRESS → COMPLETED transition (Requirement 2.5)_
    - _Preservation: Photo validation must continue to work after checklist validation_
    - _Requirements: 2.5_

  - [x] 3.6 Update Tdashboard.tsx - Remove frontend validation at service start
    - Open `web/src/pages/dashboard/Tdashboard.tsx`
    - Locate the `handleUpdateStatus` function (around line 230)
    - Remove the checklist validation block for `newStatus === 'in_progress'` (lines 230-245)
    - Add comment: `// Checklist is initialized when service starts, no validation needed`
    - _Bug_Condition: Frontend validates checklist at CONFIRMED → IN_PROGRESS transition_
    - _Expected_Behavior: Frontend SHALL NOT validate checklist at service start (Requirement 2.6)_
    - _Preservation: Photo validation and other frontend logic must remain unchanged_
    - _Requirements: 2.6_

  - [x] 3.7 Update Tdashboard.tsx - Add frontend validation at service completion
    - In the `handleUpdateStatus` function (around line 248)
    - Add checklist validation BEFORE the existing photo validation when `newStatus === 'completed'`:
    ```typescript
    // Validate checklist before In Progress -> Completed
    if (newStatus === 'completed') {
        try {
            const checklistResponse = await api.get(`/v1/technician/bookings/${bookingId}/validate-checklist`);
            if (!checklistResponse.data.isComplete) {
                const incompleteItems = checklistResponse.data.incompleteItems || [];
                setError(
                    `Cannot complete service. ${incompleteItems.length} checklist item(s) incomplete:\n` +
                    incompleteItems.slice(0, 3).join(', ') +
                    (incompleteItems.length > 3 ? '...' : '')
                );
                setTimeout(() => setError(null), 8000);
                return;
            }
        } catch (err: any) {
            setError('Failed to validate checklist');
            setTimeout(() => setError(null), 5000);
            return;
        }
    }
    ```
    - _Bug_Condition: Frontend does NOT validate checklist at IN_PROGRESS → COMPLETED transition_
    - _Expected_Behavior: Frontend SHALL validate checklist completion before allowing service completion (Requirement 2.5)_
    - _Preservation: Photo validation must continue to work after checklist validation_
    - _Requirements: 2.5_

  - [x] 3.8 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Checklist Initialization and Validation Timing
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - Verify checklist is NOT initialized at booking creation
    - Verify checklist IS initialized at CONFIRMED → IN_PROGRESS transition
    - Verify checklist validation occurs at IN_PROGRESS → COMPLETED transition
    - Verify checklist contains exactly 5 pre-service items
    - _Requirements: 2.1, 2.2, 2.5, 2.6, 2.7_

  - [x] 3.9 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Checklist Operations Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)
    - Verify booking creation continues to work correctly
    - Verify technician acceptance continues to work correctly
    - Verify photo validation continues to work correctly
    - Verify status transitions to CANCELLED/NO_SHOW continue to work correctly
    - Verify checklist item toggling continues to work correctly
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4. Checkpoint - Ensure all tests pass
  - Run all tests (bug condition exploration test + preservation tests)
  - Verify bug condition test passes (confirms fix works)
  - Verify preservation tests pass (confirms no regressions)
  - Test full workflow manually:
    1. Create booking → Verify no checklist exists
    2. Technician accepts booking (PENDING → CONFIRMED) → Verify no checklist exists
    3. Technician starts service (CONFIRMED → IN_PROGRESS) → Verify 5-item checklist is created
    4. Technician attempts to complete without checklist → Verify validation error
    5. Technician completes checklist and uploads photos → Verify service can be completed
  - Ensure all tests pass, ask the user if questions arise

## Notes

- This bugfix uses the bug condition methodology to ensure systematic validation
- Tests are written BEFORE the fix to understand the bug through counterexamples
- Preservation tests ensure existing behavior is not broken
- The fix moves checklist initialization from booking creation to service start
- The fix moves checklist validation from service start to service completion
- The fix reduces checklist items from 12 to 5 pre-service items
