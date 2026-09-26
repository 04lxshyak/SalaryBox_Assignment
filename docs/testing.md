# Testing Strategy & Automated Test Suite

## 1. Automated Test Architecture

The application contains comprehensive unit and scenario test suites located in `app/src/test/java/com/salarybox/attendance/`.

All tests run locally on the JVM without requiring an Android emulator or device, enabling fast CI/CD execution.

```
app/src/test/java/com/salarybox/attendance/
├── auth/
│   └── AuthRepositoryTest.kt            // Admin & Staff login, invalid credentials, logout
├── domain/
│   ├── StaffValidationTest.kt           // Name, ID validation, duplicate prevention
│   ├── RoleRestrictionTest.kt          // Role-based route guard tests
│   └── model/
│       └── StaffMemberTest.kt           // Domain model integrity
├── data/
│   ├── session/
│   │   └── SessionManagerTest.kt        // DataStore session serialization
│   └── database/entity/
│       └── EntityMappingTest.kt         // Room entity <-> Domain model mappings
├── face/
│   └── FaceSimilarityTest.kt            // L2 norm, cosine similarity, threshold, Person A vs B
└── AttendanceAuditScenariosTest.kt      // End-to-end policy gate audit (Scenarios A through I)
```

---

## 2. Test Suites Summary

### 1. `AttendanceAuditScenariosTest.kt` (Policy Gate Scenarios)
Verifies the complete gatekeeper matrix required by hiring specs:
* **Scenario A**: Correct face + valid location $\implies$ attendance successfully created.
* **Scenario B**: Wrong face + valid location $\implies$ rejected (`Face does not match your enrolled profile`).
* **Scenario C**: Correct face + location failure $\implies$ rejected (`Location could not be obtained`).
* **Scenario D**: Zero faces detected $\implies$ rejected (`No face detected`).
* **Scenario E**: Multiple faces in frame $\implies$ rejected (`Only one face should be visible`).
* **Scenario F**: Staff member not enrolled $\implies$ rejected (`Please ask an administrator to enroll your face`).
* **Scenario G**: Camera permission denied $\implies$ rejected (`Camera access is required`).
* **Scenario H**: Location permission denied $\implies$ rejected (`Location permission is required`).
* **Scenario I**: Double-tap protection $\implies$ atomic lock allows maximum 1 execution.

### 2. `FaceSimilarityTest.kt` (Biometric Mathematics)
* **L2 Normalization**: Confirms vectors are mapped to unit hypersphere ($\sum x_i^2 = 1.0$).
* **Cosine Similarity**: Verifies identical vectors return 1.0, orthogonal return 0.0, opposite return -1.0.
* **Decision Threshold**: Validates behavior at threshold boundary (0.70f).
* **Multi-Template Matching**: Verifies candidate embedding against 3 enrolled sample templates.
* **Person A vs. Person B**: Proves that Person A's live capture matches Person A's enrollment while Person B's capture is rejected.
* **Missing Enrollment**: Confirms `FaceVerificationResult.NoEnrollment` is returned.

### 3. `AuthRepositoryTest.kt` (Authentication & Session)
* Valid admin credentials (`admin` / `admin123`) succeed.
* Invalid admin credentials fail.
* Valid staff credentials (`EMP001` / `1234`) succeed.
* Non-existent staff or wrong password fail.
* Logout clears session.

### 4. `StaffValidationTest.kt` (Data Integrity)
* Rejects blank names or employee IDs.
* Rejects duplicate employee IDs.
* Ensures active status defaults to true.

---

## 3. Running Automated Tests

To run the complete test suite:

```powershell
# From project root:
.\gradlew.bat testDebugUnitTest
```

Expected output:
```
BUILD SUCCESSFUL in 26s
25 actionable tasks: 8 executed, 17 from cache
```

---

## 4. Manual Verification Checklist

| Scenario | Steps | Expected Result | Verified |
| :--- | :--- | :--- | :---: |
| **Clean Boot** | Install APK and launch | Launches to Login screen | [x] |
| **Admin Seeding** | Login with `admin` / `admin123` | Dashboard opens, EMP001 seeded | [x] |
| **Add Staff** | Add new staff member `EMP002` | Visible in staff list | [x] |
| **Enroll Face** | Capture 3 samples for EMP001 | Status updates to Enrolled | [x] |
| **Staff Login** | Login with `EMP001` / `1234` | Staff dashboard opens | [x] |
| **Mark Attendance** | Front camera selfie matching enrolled face | Verification success with GPS | [x] |
| **Mismatch Check** | Face camera with different person | Rejection banner shown | [x] |
| **History View** | Open Attendance History | Log entry displays with timestamp | [x] |
