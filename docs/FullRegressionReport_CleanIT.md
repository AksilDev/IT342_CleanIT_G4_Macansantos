# Full Regression Test Report - CleanIT

## Project Information

- Project: CleanIT
- Repository: https://github.com/AksilDev/IT342_CleanIT_G4_Macansantos.git
- Branch: vertical-slice-regression-refactor
- Date: 2026-05-09
- Platforms covered: Spring Boot backend, React/Vite web frontend, Android Kotlin mobile app

## Refactoring Summary

The project was refactored from technical-layer folders into vertical feature slices while keeping shared infrastructure separate. Existing API routes, screen behavior, layout resources, database entities, and application entry points were preserved.

Backend slices:

- `features/auth`: authentication controllers, DTOs, auth service, registration validators, auth strategies, user event observers, user factory
- `features/booking`: booking controllers, booking service, booking status DTOs, booking entities, booking repositories, booking enums, booking exception
- `features/catalog`: service catalog and add-on controller, entities, and repositories
- `features/users`: user/admin controllers, user entity, technician settings entity, repositories
- `shared/config`, `shared/exception`, `shared/storage`: security, JWT, application config, global exception handling, Supabase storage

Web slices:

- `features/auth`: login, registration, role selection, OAuth callback
- `features/booking`: booking creation and booking details modal
- `features/dashboard`: client, technician, and admin dashboards
- `shared/api`: Axios and Supabase clients

Mobile slices:

- `features/auth`: login, registration, auth request/response models
- `features/booking`: booking list/detail/create screens, booking adapters, booking request/response models
- `features/catalog`: service list, service/add-on/technician models and adapters
- `features/dashboard`: admin and technician dashboards plus dashboard models
- `features/home`: home and profile screens
- `shared/network`, `shared/navigation`: Retrofit API client and role navigation

## Updated Project Structure

```text
backend/src/main/java/com/G4/backend/
  BackendApplication.java
  features/
    auth/
    booking/
    catalog/
    users/
  shared/
    config/
    exception/
    storage/

web/src/
  App.tsx
  features/
    auth/
    booking/
    dashboard/
  shared/api/

mobile/app/src/main/java/edu/cit/macansantos/cleanit/
  CleanITApplication.kt
  MainActivity.kt
  features/
    auth/
    booking/
    catalog/
    dashboard/
    home/
  shared/
    navigation/
    network/
```

## Functional Requirements Coverage

| Area | Functional requirement | Covered by |
| --- | --- | --- |
| Authentication | Email/password login, registration, JWT session, Google OAuth completion, role routing | Backend auth APIs, web auth screens, mobile auth screens, mobile role navigator |
| Role access | Client, technician, and admin users navigate to role-specific dashboards | Web routes, mobile `RoleNavigator`, backend security/JWT filter |
| Service catalog | List available services, add-ons, technicians, and service add-on compatibility | Backend catalog endpoints, web booking flow, mobile service and booking screens |
| Booking creation | Client creates bookings with service, device, add-ons, date, time, address, technician assignment | Backend booking service/controller, web booking screen, mobile create booking |
| Booking management | Client views bookings, details, cancellation, rescheduling | Backend booking APIs, web dashboard/modal, mobile booking list/detail |
| Technician workflow | Pending/assigned bookings, accept/reject, status updates, checklist, required before/after photos | Backend technician booking APIs, web technician dashboard, mobile technician dashboard |
| Admin workflow | View dashboard statistics, bookings, user verification, cancellation/status oversight | Backend admin/user APIs, web admin dashboard, mobile admin dashboard |
| Data integrity | Booking child records are preserved during normal operations and removed correctly when deleting bookings | Backend cascade-delete and preservation tests |
| Regression safety | Application starts and compiles after vertical slice package moves | Backend tests, web production build, mobile Kotlin compile/unit test |

## Test Plan

| Test ID | Requirement | Test procedure | Automated evidence |
| --- | --- | --- | --- |
| TP-01 | Backend application context | Run backend Spring Boot tests and verify application context loads | `backend/target/surefire-reports/com.G4.backend.BackendApplicationTests.txt` |
| TP-02 | Booking checklist initialization | Run checklist initialization regression tests | `ChecklistInitializationBugTest`, `ChecklistPreservationPropertyTest` reports |
| TP-03 | Booking cascade deletion | Delete bookings with photos, checklist rows, add-ons, and all child types | `BookingCascadeDeleteBugTest` report |
| TP-04 | Booking non-deletion behavior | Create/query/update child records and delete child records independently | `BookingCascadeDeletePreservationPropertyTest` report |
| TP-05 | Web route/build integrity | Run Vite production build | `web/dist/index.html`, `web/dist/assets/*` |
| TP-06 | Mobile Kotlin/package integrity | Compile Android debug Kotlin sources | Gradle `:app:compileDebugKotlin` successful output |
| TP-07 | Mobile unit tests | Run Android debug unit tests | `mobile/app/build/test-results/testDebugUnitTest/TEST-edu.cit.macansantos.cleanit.ExampleUnitTest.xml` |
| TP-08 | Manual smoke procedures | Login/register, role dashboard navigation, create booking, update booking, admin verification | Manual procedures listed below |

## Manual Regression Steps

1. Register a client, technician, and admin account.
2. Log in as each role and verify redirection to the correct dashboard.
3. As a client, open services, choose a service/add-ons, create a booking, and verify it appears in the client dashboard.
4. As a technician, view assigned bookings, accept a booking, start service, complete checklist, upload before/after photos, and complete service.
5. As a client, open booking details and verify status, photos, schedule, and reschedule/cancel controls.
6. As an admin, verify dashboard statistics, booking list, user verification controls, and booking detail modal.
7. Confirm invalid states are blocked: incompatible add-ons, completing service without checklist/photos, unverified users booking or accepting work.

## Automated Test Evidence

Commands executed on 2026-05-09:

| Command | Result |
| --- | --- |
| `backend/.mvnw.cmd -DskipTests compile` | Passed |
| `backend/.mvnw.cmd test` | Passed: 18 tests, 0 failures, 0 errors |
| `web/npm run build` | Passed: Vite production build created `web/dist` |
| `mobile/gradlew.bat :app:compileDebugKotlin` | Passed |
| `mobile/gradlew.bat :app:testDebugUnitTest` | Passed |

Evidence files:

- Backend Surefire XML/text reports: `backend/target/surefire-reports/`
- Mobile unit test XML report: `mobile/app/build/test-results/testDebugUnitTest/`
- Web production build artifacts: `web/dist/`

## Regression Test Results

| Area | Result | Notes |
| --- | --- | --- |
| Backend compile | Passed | All moved packages compile under feature slices |
| Backend tests | Passed | 18 tests passed after cascade-delete fix |
| Web build | Passed | Build completed; Vite reported only existing chunk-size/module-type warnings |
| Mobile compile | Passed | Kotlin compile passed with existing deprecation/unused-variable warnings |
| Mobile unit tests | Passed | Debug unit test task completed successfully |

## Issues Found

| Issue | Severity | Status | Evidence |
| --- | --- | --- | --- |
| Booking deletion did not reliably remove checklist/add-on child rows when child collections were not attached to the in-memory booking aggregate | High | Fixed | Initial `backend/.mvnw.cmd test` failed 3 cascade-delete cases |
| First mobile unit-test run could not access Gradle wrapper lock under user `.gradle` directory | Environment | Resolved | Reran with permission; test task passed |

## Fixes Applied

- Refactored backend, web, and mobile code into feature-oriented packages/folders.
- Updated backend package declarations and imports for feature slices.
- Updated web route imports and API-client paths.
- Updated Android package declarations, manifest activity names, resource imports, cross-feature Activity imports, Retrofit model imports, and role navigation imports.
- Moved mobile booking models out of the catalog model file into the booking slice.
- Fixed booking deletion by making `BookingRepository.delete(Booking)` explicitly delete booking photos, checklist rows, and add-on join rows before deleting the booking row.

## Submission Notes

- Branch to push: `vertical-slice-regression-refactor`
- Repository link: https://github.com/AksilDev/IT342_CleanIT_G4_Macansantos.git
- PDF filename: `FullRegressionReport_CleanIT.pdf`
- Supporting automated evidence is available in the build/test report folders listed above.
