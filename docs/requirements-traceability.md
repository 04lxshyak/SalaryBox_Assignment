# Requirements Traceability

Status reflects evidence available on 2026-09-26. A `PASS` requires verified execution; no device or emulator evidence is available in this workspace.

| Requirement | Implementation | Relevant files | Verification method | Status |
|---|---|---|---|---|
| Admin and staff login | Separate role flows with DataStore session persistence | `data/repository/AuthRepositoryImpl.kt`, `presentation/auth/LoginViewModel.kt` | Source inspection; tests not executed | PARTIAL |
| Admin views and adds staff | Room-backed staff data, validation, and unique employee ID | `presentation/admin/`, `data/database/entity/StaffEntity.kt` | Source inspection; UI not executed | PARTIAL |
| Admin opens staff profile | Profile and enrollment navigation | `presentation/admin/staffprofile/`, `core/navigation/AppNavHost.kt` | Source inspection; UI not executed | PARTIAL |
| Admin enrolls face | Three validated samples create an averaged, normalized MobileFaceNet template | `FaceEnrollmentViewModel.kt`, `RealFaceRecognitionEngine.kt` | Source inspection; camera not tested | PARTIAL |
| Attendance requires face match | Candidate embedding is matched to the stored template via cosine similarity threshold 0.70 | `FaceRecognitionEngine.kt`, `AttendanceMarkViewModel.kt` | Source inspection; synthetic math tests exist but were not run | PARTIAL |
| Timestamp, selfie, location stored | Current time, internal selfie file path, fresh fused location persisted after a match | `AttendanceMarkViewModel.kt`, `InternalFileStorage.kt`, `LocationService.kt`, `AttendanceEntity.kt` | Source inspection; database not exercised | PARTIAL |
| APK | Historical APK exists at `release/attendance-app.apk` | `release/attendance-app.apk` | Nonzero file only; source alignment and launch unverified | PARTIAL |
| README and AI conversation export | README plus template/disclosure documents present | `README.md`, `docs/ai-conversation-export*` | Content inspected | PARTIAL |

## Audit Corrections

- The attendance screen now requests location permission before camera capture.
- Attendance requires a fresh current location and rejects a location timeout instead of using a stale last-known coordinate.
- Attendance retries face-engine initialization after a failed application warmup and fails closed if it cannot initialize.

## Required Verification

Before submission, run a fresh build and test Admin login, staff creation, enrollment, Person A match, Person B mismatch, all permission failures, persisted timestamp/selfie/location, and attendance history on a device or emulator.
