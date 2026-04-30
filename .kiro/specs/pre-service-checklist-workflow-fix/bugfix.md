# Bugfix Requirements Document

## Introduction

The pre-service checklist workflow is currently incorrect. The checklist is initialized too early (when a booking is created) and can be checked at any time, even before the service starts. Additionally, validation occurs at the wrong transition point. This bug breaks the intended workflow where the checklist should only become available after the technician presses "Start Service" and must be completed before the service can be marked as completed.

**Impact:** Technicians can check off pre-service items before actually starting the service, undermining the purpose of the checklist as a pre-service verification tool. The workflow does not enforce the correct sequence of operations.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a booking is created THEN the system initializes the pre-service checklist immediately (too early)

1.2 WHEN a booking is in PENDING or CONFIRMED status THEN the system allows technicians to check checklist items at any time

1.3 WHEN transitioning from CONFIRMED to IN_PROGRESS status THEN the system validates that all checklist items are completed

1.4 WHEN transitioning from IN_PROGRESS to COMPLETED status THEN the system does not validate checklist completion (validation happens at wrong transition)

1.5 WHEN the checklist is initialized THEN the system creates 12 checklist items instead of the required 5 pre-service items

### Expected Behavior (Correct)

2.1 WHEN a booking is created THEN the system SHALL NOT initialize any checklist

2.2 WHEN transitioning from CONFIRMED to IN_PROGRESS status (pressing "Start Service") THEN the system SHALL initialize a 5-item pre-service checklist

2.3 WHEN a booking is in CONFIRMED status THEN the system SHALL NOT allow technicians to check any checklist items (checklist does not exist yet)

2.4 WHEN a booking is in IN_PROGRESS status THEN the system SHALL allow the assigned technician to check pre-service checklist items

2.5 WHEN transitioning from IN_PROGRESS to COMPLETED status THEN the system SHALL validate that all 5 pre-service checklist items are completed

2.6 WHEN transitioning from CONFIRMED to IN_PROGRESS status THEN the system SHALL NOT validate checklist completion (checklist is being created at this point)

2.7 WHEN initializing the pre-service checklist THEN the system SHALL create exactly 5 checklist items with the following labels:
   - "Verify location is valid and searchable"
   - "Inspect tools for service are clean and working"
   - "Client available and gives consent"
   - "Test device is working before beginning physical service"
   - "Review service requirements with client"

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a technician accepts a booking (PENDING → CONFIRMED) THEN the system SHALL CONTINUE TO validate technician availability and time slot conflicts

3.2 WHEN transitioning from IN_PROGRESS to COMPLETED status THEN the system SHALL CONTINUE TO validate that required photos (before and after) are uploaded

3.3 WHEN a technician toggles a checklist item THEN the system SHALL CONTINUE TO verify that the technician is assigned to the booking

3.4 WHEN retrieving booking checklist THEN the system SHALL CONTINUE TO return checklist items with their completion status

3.5 WHEN a booking is in any status other than IN_PROGRESS THEN the system SHALL CONTINUE TO enforce appropriate status transition rules

3.6 WHEN a booking is cancelled or marked as no-show THEN the system SHALL CONTINUE TO handle these transitions without checklist validation
