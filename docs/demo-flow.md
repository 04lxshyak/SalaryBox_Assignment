# Demo Flow & Evaluator Walkthrough

This guide provides the exact test script for evaluating the application within 5–10 minutes.

---

## 1. Demo Credentials

| Role | Username / Identifier | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | Full access to staff list, add staff, face enrollment. |
| **Staff** | `EMP001` | `1234` | Seeded demo employee (*Demo Employee*). No pre-enrolled face. |
| **Custom Staff** | Any added `employeeId` | `1234` | Added by Admin in Staff List. |

---

## 2. Standard Happy Path (5-Minute Demo)

### Step 1: Admin Login & Face Enrollment
1. Launch the app (`release/attendance-app.apk` or `app-debug.apk`).
2. On Login screen, select **Admin** tab.
3. Enter `admin` / `admin123` and tap **Login as Admin**.
4. You arrive at the **Admin Dashboard** showing summary metrics:
   * Total Staff: 1
   * Enrolled Faces: 0
5. Tap **Manage Staff** to open the Staff List.
6. Tap on **Demo Employee (EMP001)** to open the Staff Profile.
   * Notice the status badge: *"Face Not Enrolled"*.
7. Tap **Enroll Face**.
8. Grant Camera permission when prompted.
9. Position your face in the oval guide.
   * Tap **Capture Sample 1**.
   * Turn head slightly or smile, tap **Capture Sample 2**.
   * Look straight ahead, tap **Capture Sample 3**.
10. The system displays *"Face Enrolled Successfully!"*. Tap **Done**.
11. Tap the **Logout** icon in the top right.

### Step 2: Staff Login & Real Face Verification
1. On Login screen, select **Staff** tab.
2. Enter `EMP001` / `1234` and tap **Login as Staff**.
3. You arrive at the **Staff Dashboard**:
   * Welcome, Demo Employee.
   * Notice that **Mark Attendance** is now enabled.
4. Tap **Mark Attendance**.
5. Grant Camera and Location permissions when prompted.
6. The front camera displays the face guide with status:
   * *"Position your face within the guide and tap Verify Attendance."*
7. Tap **Verify & Mark Attendance**.
8. Observe the processing states:
   * *Detecting Face...* $\implies$ *Generating Embedding...* $\implies$ *Verifying Identity...* $\implies$ *Acquiring GPS location...*
9. **Attendance Marked!** Success screen appears showing:
   * Employee Name: Demo Employee (EMP001)
   * Exact Timestamp (e.g., Saturday, 26 Sep 2026 at 10:45:12 PM)
   * Identity Verification: VERIFIED (Confidence: ~85-95%)
   * Location: Exact Latitude, Longitude, and reverse geocoded locality.
10. Tap **Back to Dashboard**.
11. View Today's Attendance status and tap **View History** to inspect the recorded logs.

---

## 3. Negative & Security Test Cases (Red Team Audit)

### Test Case A: Face Mismatch (Person B Attempts Attendance)
1. Keep `EMP001` enrolled with Person A's face.
2. Log in as `EMP001`.
3. Have Person B (a different person) face the camera and tap **Verify & Mark Attendance**.
4. **Expected Result**: 
   * Identity verification fails.
   * Error message: *"Face does not match your enrolled profile."*
   * **No attendance record is written to the database.**

### Test Case B: Attendance Without Face Enrollment
1. Log in as Admin.
2. Add a new staff member (e.g., `EMP002`, name "Alex Smith"). Do NOT enroll their face.
3. Logout and log in as `EMP002` / `1234`.
4. Tap **Mark Attendance**.
5. **Expected Result**:
   * Blocked immediately.
   * Message: *"Please ask an administrator to enroll your face."*

### Test Case C: Location Denied / Disabled
1. Log in as `EMP001` (with face enrolled).
2. Disable device GPS / Location services in phone settings.
3. Attempt to mark attendance with matching face.
4. **Expected Result**:
   * Location acquisition fails.
   * Error message: *"Location could not be obtained. Attendance was not recorded."*
   * Database remains unchanged.

### Test Case D: Multiple Faces in Frame
1. Open the camera for attendance or enrollment.
2. Have two people look at the camera simultaneously.
3. **Expected Result**:
   * Rejected: *"Only one face should be visible. Detected 2 faces."*

### Test Case E: Rapid Double-Tap (Concurrency Protection)
1. Tap the **Verify & Mark Attendance** button rapidly multiple times.
2. **Expected Result**:
   * Concurrency lock debounces the trigger.
   * Only 1 operation executes; exactly 1 attendance entry is created.
