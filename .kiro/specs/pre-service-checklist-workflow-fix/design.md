# Pre-Service Checklist Workflow Fix - Bugfix Design

## Overview

The pre-service checklist workflow has two critical timing bugs: (1) the checklist is initialized too early (at booking creation instead of when service starts), and (2) validation occurs at the wrong transition point (CONFIRMED → IN_PROGRESS instead of IN_PROGRESS → COMPLETED). Additionally, the checklist contains 12 items instead of the required 5 pre-service items. This fix will move checklist initialization to the CONFIRMED → IN_PROGRESS transition, move validation to the IN_PROGRESS → COMPLETED transition, reduce checklist items to 5 pre-service items, and update the frontend to support the new workflow.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when checklist is initialized at booking creation and validated at CONFIRMED → IN_PROGRESS transition
- **Property (P)**: The desired behavior - checklist should be initialized at CONFIRMED → IN_PROGRESS and validated at IN_PROGRESS → COMPLETED
- **Preservation**: Existing photo validation, technician assignment validation, and status transition rules that must remain unchanged
- **initializeBookingChecklist**: The method in `BookingService.java` that creates checklist items for a booking
- **updateBookingStatus**: The method in `BookingService.java` that handles status transitions and validation
- **BookingStatus**: Enum representing booking lifecycle states (PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)
- **Pre-service checklist**: A 5-item checklist that technicians must complete before marking service as completed

## Bug Details

### Bug Condition

The bug manifests when a booking is created or when a technician attempts to transition booking status. The `createBooking` method initializes the checklist too early, and the `updateBookingStatus` method validates checklist completion at the wrong transition point.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type BookingOperation
  OUTPUT: boolean
  
  RETURN (input.operation == "createBooking" AND checklistInitializedImmediately())
         OR (input.operation == "updateStatus" 
             AND input.oldStatus == CONFIRMED 
             AND input.newStatus == IN_PROGRESS 
             AND checklistValidationPerformed())
         OR (input.operation == "updateStatus"
             AND input.oldStatus == IN_PROGRESS
             AND input.newStatus == COMPLETED
             AND NOT checklistValidationPerformed())
         OR (checklistItemCount() == 12 AND NOT checklistItemCount() == 5)
END FUNCTION
```

### Examples

- **Example 1**: Client creates a booking → System initializes 12-item checklist immediately (WRONG: should not initialize yet)
- **Example 2**: Technician presses "Start Service" (CONFIRMED → IN_PROGRESS) → System validates checklist completion (WRONG: checklist should be created at this point, not validated)
- **Example 3**: Technician presses "Complete Service" (IN_PROGRESS → COMPLETED) → System does not validate checklist (WRONG: should validate checklist completion here)
- **Example 4**: Technician views checklist for IN_PROGRESS booking → System shows 12 items (WRONG: should show only 5 pre-service items)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Photo validation at IN_PROGRESS → COMPLETED transition must continue to work (validatePhotosUploaded)
- Technician assignment validation must continue to work (validateStatusChangePermission)
- Status transition rules must remain unchanged (canTransitionTo logic)
- Checklist item toggling must continue to verify technician assignment
- Booking creation flow (client validation, add-on compatibility) must remain unchanged
- Technician acceptance flow (availability check, workload limit, time slot conflicts) must remain unchanged

**Scope:**
All operations that do NOT involve checklist initialization or checklist validation should be completely unaffected by this fix. This includes:
- Booking creation (except removing checklist initialization)
- Technician acceptance (PENDING → CONFIRMED transition)
- Photo upload and validation
- Status transitions to CANCELLED or NO_SHOW
- Checklist item retrieval and toggling (once checklist exists)

## Hypothesized Root Cause

Based on the bug description and code analysis, the root causes are:

1. **Incorrect Initialization Timing**: The `createBooking` method calls `initializeBookingChecklist(savedBooking)` immediately after saving the booking, which is too early. The checklist should only be created when the technician starts the service.

2. **Incorrect Validation Timing**: The `updateBookingStatus` method validates checklist completion at the CONFIRMED → IN_PROGRESS transition (line ~350 in BookingService.java), but this is when the checklist should be initialized, not validated. The validation should occur at IN_PROGRESS → COMPLETED transition.

3. **Wrong Checklist Items**: The `DataInitializer.java` creates 12 checklist items (lines 115-128), but only 5 pre-service items are needed. The current items include post-service activities like "Take after-service photos" and "Obtain client confirmation" which should not be in a pre-service checklist.

4. **Missing Initialization Logic**: There is no code to initialize the checklist at the CONFIRMED → IN_PROGRESS transition point.

## Correctness Properties

Property 1: Bug Condition - Checklist Initialization and Validation Timing

_For any_ booking status transition where the transition is from CONFIRMED to IN_PROGRESS, the fixed updateBookingStatus function SHALL initialize a 5-item pre-service checklist for that booking, and SHALL NOT validate checklist completion at this transition point.

**Validates: Requirements 2.1, 2.2, 2.6, 2.7**

Property 2: Bug Condition - Validation at Completion

_For any_ booking status transition where the transition is from IN_PROGRESS to COMPLETED, the fixed updateBookingStatus function SHALL validate that all 5 pre-service checklist items are completed, and SHALL throw a BookingException if any items are incomplete.

**Validates: Requirements 2.5**

Property 3: Preservation - Photo Validation Unchanged

_For any_ booking status transition from IN_PROGRESS to COMPLETED, the fixed code SHALL continue to validate photo uploads exactly as the original code does, preserving the existing photo validation logic.

**Validates: Requirements 3.2**

Property 4: Preservation - Non-Checklist Operations Unchanged

_For any_ booking operation that does NOT involve checklist initialization or validation (booking creation without checklist, technician acceptance, photo upload, status transitions to CANCELLED/NO_SHOW), the fixed code SHALL produce exactly the same behavior as the original code.

**Validates: Requirements 3.1, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `backend/src/main/java/com/G4/backend/service/BookingService.java`

**Function**: `createBooking`, `updateBookingStatus`, `initializeBookingChecklist`

**Specific Changes**:

1. **Remove Early Checklist Initialization**:
   - In `createBooking` method (around line 130), remove the call to `initializeBookingChecklist(savedBooking)`
   - This prevents checklist creation at booking time

2. **Move Checklist Initialization to Start Service**:
   - In `updateBookingStatus` method, add checklist initialization logic when transitioning from CONFIRMED → IN_PROGRESS
   - Add code after line ~340 (before the existing validation block):
   ```java
   // Initialize checklist when starting service (CONFIRMED -> IN_PROGRESS)
   if (oldStatus == BookingStatus.CONFIRMED && newStatus == BookingStatus.IN_PROGRESS) {
       initializeBookingChecklist(booking);
   }
   ```

3. **Remove Incorrect Validation at Start Service**:
   - In `updateBookingStatus` method, remove the checklist validation block at CONFIRMED → IN_PROGRESS transition (lines ~350-361)
   - This validation is incorrect because the checklist is being created at this point

4. **Add Validation at Complete Service**:
   - In `updateBookingStatus` method, move the checklist validation to IN_PROGRESS → COMPLETED transition
   - Add validation before the existing photo validation (around line 363):
   ```java
   // Validate checklist completion before completing service (IN_PROGRESS -> COMPLETED)
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

5. **Update Checklist Items to 5 Pre-Service Items**:
   - In `DataInitializer.java`, replace the 12-item checklist (lines 115-128) with 5 pre-service items:
   ```java
   List<String> checklistLabels = Arrays.asList(
       "Verify location is valid and searchable",
       "Inspect tools for service are clean and working",
       "Client available and gives consent",
       "Test device is working before beginning physical service",
       "Review service requirements with client"
   );
   ```

**File**: `web/src/pages/dashboard/Tdashboard.tsx`

**Component**: `Tdashboard`, `handleUpdateStatus`

**Specific Changes**:

6. **Remove Frontend Validation at Start Service**:
   - In `handleUpdateStatus` function (around line 230), remove the checklist validation block for `newStatus === 'in_progress'` (lines 230-245)
   - This validation is incorrect because the checklist doesn't exist yet at this point

7. **Add Frontend Validation at Complete Service**:
   - In `handleUpdateStatus` function, add checklist validation before the existing photo validation when `newStatus === 'completed'`
   - Add validation block before line 248:
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

8. **Update Frontend to Handle Empty Checklist**:
   - In `fetchChecklist` function (around line 320), handle the case where checklist is empty (booking not started yet)
   - The current implementation already handles this correctly by setting `setChecklist([])` on error

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Write tests that create bookings and attempt status transitions, observing when checklist is initialized and validated. Run these tests on the UNFIXED code to observe failures and understand the root cause.

**Test Cases**:
1. **Early Initialization Test**: Create a booking and verify checklist is initialized immediately (will pass on unfixed code, should fail on fixed code)
2. **Wrong Validation Point Test**: Transition CONFIRMED → IN_PROGRESS with incomplete checklist and verify validation occurs (will pass on unfixed code, should fail on fixed code)
3. **Missing Validation Test**: Transition IN_PROGRESS → COMPLETED with incomplete checklist and verify no validation occurs (will pass on unfixed code, should fail on fixed code)
4. **Wrong Item Count Test**: Create a booking and verify checklist has 12 items (will pass on unfixed code, should fail on fixed code)

**Expected Counterexamples**:
- Checklist is initialized at booking creation time (too early)
- Checklist validation occurs at CONFIRMED → IN_PROGRESS transition (wrong point)
- No checklist validation occurs at IN_PROGRESS → COMPLETED transition (missing validation)
- Checklist contains 12 items instead of 5 pre-service items

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL bookingOperation WHERE isBugCondition(bookingOperation) DO
  result := performOperation_fixed(bookingOperation)
  ASSERT expectedBehavior(result)
END FOR
```

**Test Cases**:
1. **Correct Initialization Timing**: Create a booking → Verify no checklist exists → Transition to IN_PROGRESS → Verify 5-item checklist is created
2. **Correct Validation at Completion**: Start service → Leave checklist incomplete → Attempt to complete → Verify validation error
3. **Successful Completion with Complete Checklist**: Start service → Complete all 5 checklist items → Upload photos → Complete service → Verify success
4. **Correct Item Count**: Start service → Verify checklist has exactly 5 pre-service items with correct labels

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL bookingOperation WHERE NOT isBugCondition(bookingOperation) DO
  ASSERT performOperation_original(bookingOperation) = performOperation_fixed(bookingOperation)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs

**Test Plan**: Observe behavior on UNFIXED code first for non-checklist operations, then write property-based tests capturing that behavior.

**Test Cases**:
1. **Booking Creation Preservation**: Verify booking creation (without checklist) continues to work with client validation, add-on compatibility checks
2. **Technician Acceptance Preservation**: Verify PENDING → CONFIRMED transition continues to validate availability, workload, time slots
3. **Photo Validation Preservation**: Verify IN_PROGRESS → COMPLETED transition continues to validate photo uploads
4. **Status Transition Preservation**: Verify transitions to CANCELLED and NO_SHOW continue to work without checklist validation
5. **Checklist Toggle Preservation**: Verify checklist item toggling continues to validate technician assignment (once checklist exists)

### Unit Tests

- Test checklist initialization at CONFIRMED → IN_PROGRESS transition
- Test checklist validation at IN_PROGRESS → COMPLETED transition
- Test that checklist is NOT initialized at booking creation
- Test that checklist validation does NOT occur at CONFIRMED → IN_PROGRESS
- Test that checklist contains exactly 5 items with correct labels
- Test error handling when attempting to complete service with incomplete checklist

### Property-Based Tests

- Generate random booking workflows and verify checklist is always initialized at correct time
- Generate random checklist completion states and verify validation occurs at correct transition
- Generate random status transitions and verify preservation of non-checklist validations
- Test that all booking operations preserve existing behavior when checklist is not involved

### Integration Tests

- Test full booking workflow: create → accept → start (checklist created) → complete checklist → upload photos → complete service
- Test that technician cannot complete service without completing checklist
- Test that technician cannot check checklist items before starting service (checklist doesn't exist)
- Test that frontend correctly handles empty checklist for CONFIRMED bookings
- Test that frontend correctly validates checklist at completion time
