# Requirements Traceability Matrix

This document tracks all hiring assignment requirements against their architectural implementation, source code files, and automated/manual verification methods.

---

## 1. Authentication & Role-Based Access Control

| Requirement | Implementation Detail | Relevant Source Files | Verification Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Admin Login** | Credentials `admin` / `admin123`. Fixed role ADMIN, persists session. | `data/repository/AuthRepositoryImpl.kt`<br>`presentation/auth/LoginViewModel.kt`<br>`presentation/auth/LoginScreen.kt` | `AuthRepositoryTest.kt`<br>Manual login check | **PASS** |
| **Staff Login** | Employee ID + default password `1234`. Verifies employee exists and active. | `data/repository/AuthRepositoryImpl.kt`<br>`presentation/auth/LoginViewModel.kt` | `AuthRepositoryTest.kt`<br>Manual login with `EMP001` | **PASS** |
| **Session Persistence** | Jetpack DataStore Preferences stores `userId`, `userName`, `userRole`, `staffId`. | `data/session/SessionManager.kt`<br>`MainActivity.kt` | `SessionManagerTest.kt`<br>Process kill & cold start | **PASS** |
| **Role-Aware Routing** | Admin redirected to Admin Dashboard; Staff redirected to Staff Dashboard. | `MainActivity.kt`<br>`core/navigation/AppNavHost.kt` | `RoleRestrictionTest.kt`<br>Navigation flow audit | **PASS** |
| **Logout** | Clears active session in DataStore; navigates back to Login clearing backstack. | `presentation/admin/dashboard/AdminDashboardViewModel.kt`<br>`presentation/staff/StaffDashboardViewModel.kt` | `AuthRepositoryTest.kt` | **PASS** |

---

## 2. Admin Workflow

| Requirement | Implementation Detail | Relevant Source Files | Verification Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| **View Staff List** | Flow-driven list of staff members with active status & enrollment badge. | `presentation/admin/stafflist/StaffListScreen.kt`<br>`presentation/admin/stafflist/StaffListViewModel.kt`<br>`data/database/dao/StaffDao.kt` | `EntityMappingTest.kt`<br>Manual UI verification | **PASS** |
| **Add Staff Member** | Form validating Name and unique Employee ID. Disallows duplicates. | `presentation/admin/addstaff/AddStaffScreen.kt`<br>`presentation/admin/addstaff/AddStaffViewModel.kt`<br>`domain/repository/StaffRepository.kt` | `StaffValidationTest.kt`<br>Database unique constraint | **PASS** |
| **Staff Profile** | Shows staff details, enrollment status, toggle active state, face preview. | `presentation/admin/staffprofile/StaffProfileScreen.kt`<br>`presentation/admin/staffprofile/StaffProfileViewModel.kt` | Manual navigation & state check | **PASS** |
| **Face Enrollment UI** | CameraX front-facing preview, oval guide, 3-step sample capture, progress. | `presentation/admin/enrollment/FaceEnrollmentScreen.kt`<br>`presentation/admin/enrollment/FaceEnrollmentViewModel.kt`<br>`camera/CameraPreviewView.kt` | Unit tests on enrollment averaging<br>Manual test with camera | **PASS** |

---

## 3. Real On-Device Face Recognition Pipeline

| Requirement | Implementation Detail | Relevant Source Files | Verification Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Face Detection** | Google ML Kit Face Detection. Rejects 0 or >1 faces, checks head pose (yaw, pitch, roll <= 20°). | `face/RealFaceRecognitionEngine.kt`<br>`face/FaceDetectionResult.kt` | `AttendanceAuditScenariosTest.kt`<br>(Scenarios D, E) | **PASS** |
| **Face Preprocessing & Alignment** | Crops bounding box with 15% margin, scales to 112x112, normalizes pixels to [-1, 1]. | `face/RealFaceRecognitionEngine.kt`<br>`face/FaceRecognitionConfig.kt` | TFLite tensor input dimension validation | **PASS** |
| **Face Embedding Model** | MobileFaceNet TFLite model running locally. Generates 192-dimensional vector. | `app/src/main/assets/mobilefacenet.tflite`<br>`face/RealFaceRecognitionEngine.kt` | File header inspection (`TFL3`)<br>Model tensor output check | **PASS** |
| **Vector Normalization** | L2 Normalization ensures unit length: `v / max(norm, 1e-10)`. | `face/RealFaceRecognitionEngine.kt`<br>`face/FaceSimilarityTest.kt` | `FaceSimilarityTest.kt` (`l2 normalization produces unit norm vector`) | **PASS** |
| **Face Matching & Verification** | Cosine similarity against enrolled template. Threshold = 0.70f. | `face/FaceRecognitionEngine.kt`<br>`face/FaceRecognitionConfig.kt`<br>`face/FaceVerificationResult.kt` | `FaceSimilarityTest.kt` (Person A matches, Person B fails) | **PASS** |
| **Fail Closed Security** | If model missing, uninitialized, or low similarity, fails closed. No demo bypass. | `face/RealFaceRecognitionEngine.kt`<br>`presentation/staff/AttendanceMarkViewModel.kt` | `AttendanceAuditScenariosTest.kt` (Scenarios B, F) | **PASS** |

---

## 4. Staff Attendance Workflow & Integrity

| Requirement | Implementation Detail | Relevant Source Files | Verification Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Attendance Policy Gates** | Must satisfy: session + enrolled + 1 face + verified match + location acquired. | `presentation/staff/AttendanceMarkViewModel.kt` | `AttendanceAuditScenariosTest.kt`<br>(Scenarios A through I) | **PASS** |
| **Location Acquisition** | FusedLocationProviderClient with 10s strict timeout, accuracy check, reverse geocode. | `location/LocationService.kt`<br>`location/LocationResult.kt` | `AttendanceAuditScenariosTest.kt`<br>(Scenarios C, H) | **PASS** |
| **Selfie Storage** | Saved to app-private storage (`context.filesDir/selfies`). Stored in Room. | `data/storage/InternalFileStorage.kt`<br>`data/database/entity/AttendanceEntity.kt` | File existence and path persistence | **PASS** |
| **Duplicate Protection** | AtomicBoolean concurrency lock debounces capture CTA; prevents duplicate records. | `presentation/staff/AttendanceMarkViewModel.kt` | `AttendanceAuditScenariosTest.kt` (Scenario I) | **PASS** |
| **Attendance Success Screen** | Displays employee name, formatted timestamp, confidence %, coordinates & address. | `presentation/staff/AttendanceMarkScreen.kt` | UI verification of AttendanceSuccessView | **PASS** |
| **Attendance History** | Shows chronological history of past attendance records for the staff member. | `presentation/staff/AttendanceHistoryScreen.kt`<br>`presentation/staff/AttendanceHistoryViewModel.kt` | `AttendanceDaoTest` & UI checks | **PASS** |

---

## 5. Persistence & Data Layer

| Requirement | Implementation Detail | Relevant Source Files | Verification Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Room Database** | SQLite database with foreign keys (CASCADE delete) and indices on employeeId, staffId, timestamp. | `data/database/AppDatabase.kt`<br>`data/database/entity/*.kt`<br>`data/database/dao/*.kt` | `EntityMappingTest.kt`<br>KSP code generation | **PASS** |
| **Pre-populated Seed** | Seeds `EMP001` / `Demo Employee` on database create via Room callback. | `data/database/AppDatabase.kt` | `AuthRepositoryTest.kt` | **PASS** |
| **Storage Hardening** | Sanitized filenames, orphan selfie cleanup, deletion on re-enrollment. | `data/storage/InternalFileStorage.kt` | Unit tests & IO failure guards | **PASS** |

---

## 6. Deliverables & Submission Artifacts

| Deliverable | Location | Status |
| :--- | :--- | :--- |
| **Release APK** | `release/attendance-app.apk` | **READY (72.6 MB, Signed)** |
| **Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` | **READY (79.5 MB)** |
| **README** | `README.md` | **COMPLETE** |
| **Requirements Traceability** | `docs/requirements-traceability.md` | **COMPLETE** |
| **Architecture Documentation** | `docs/architecture.md` | **COMPLETE** |
| **Face Verification Specs** | `docs/face-verification.md` | **COMPLETE** |
| **Demo Flow & Test Plan** | `docs/demo-flow.md` | **COMPLETE** |
| **Testing Guide** | `docs/testing.md` | **COMPLETE** |
| **Security & Privacy Specs** | `docs/security.md` | **COMPLETE** |
| **AI Conversation Export Docs** | `docs/ai-conversation-export.md`<br>`docs/ai-conversation-export-template.json`<br>`docs/ai-build-summary.md` | **COMPLETE** |
