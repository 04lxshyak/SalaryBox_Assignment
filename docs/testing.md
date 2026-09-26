# Testing

## Automated Coverage Present

The repository contains JVM tests for face-similarity mathematics, role restrictions, session serialization, staff model validation, and simulated attendance policy scenarios under `app/src/test/java/com/salarybox/attendance/`.

The policy scenario tests are simulations. They do not run CameraX, ML Kit, TensorFlow Lite, Room, or device location services end to end.

## Commands

```powershell
.\gradlew.bat clean
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

## Current Status

The commands were attempted in this environment, but Gradle could not start because Java failed to establish a required local loopback connection. No build, lint, or test result is claimed.

## Required Device Checks

1. Admin sign-in, staff creation, and duplicate-ID rejection.
2. Three-sample front-camera enrollment.
3. Enrolled person succeeds; a different person is rejected without an attendance row.
4. Attendance persists a current timestamp, internal selfie path, and fresh location.
5. Denied camera/location permissions, disabled location, no/multiple face, missing enrollment, model unavailability, and rapid repeated taps all reject safely.
