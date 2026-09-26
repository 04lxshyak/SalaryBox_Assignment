# Security & Privacy Architecture

This document details the security principles, data protection measures, and privacy controls enforced across the application.

---

## 1. Core Security Principles

### Principle 1: Zero Cloud / 100% Local Execution
* Biometric data (face images, detection landmarks, embedding feature vectors) **never leaves the physical device**.
* The application has no backend, no cloud networking, and no analytics telemetry transmitting personal information.
* Model inference (ML Kit and MobileFaceNet) runs completely offline on the local CPU/NPU.

### Principle 2: Fail-Closed Enforcement
* If the face recognition model fails to initialize, if image processing encounters an exception, or if similarity is below the threshold, **verification fails immediately**.
* There is no fallback "demo mode" or bypass flag.
* If GPS location cannot be acquired within 10 seconds, the operation aborts and **no attendance record is persisted**.

### Principle 3: No Biometric Exposure in Logs or UI
* Face embedding vectors (192 floats) are never logged via `Log.d`, `println`, or error stack traces.
* The UI displays only human-readable feedback and verification percentages—never raw vectors or internal model weights.
* Unhandled technical exceptions are sanitized before being displayed to users.

### Principle 4: App-Private Storage Isolation
* All captured face images and selfies are saved strictly in `context.filesDir` (e.g., `context.filesDir/selfies/` and `context.filesDir/enrollments/`).
* Android's Linux sandbox prevents other applications on the device from accessing or reading these files.
* Biometric image directories are excluded from external storage and version control (`.gitignore`).

---

## 2. Storage Hardening & Integrity

Implemented in `data/storage/InternalFileStorage.kt`:
* **Sanitized Filenames**: File names use strict alphanumeric filters and millisecond timestamps:
  `selfie_{staffId}_{timestamp}.jpg`
* **Cascade Deletion**: When a staff member is deleted from Room, foreign key constraints (`ON DELETE CASCADE`) remove their enrollment and attendance rows.
* **Orphan Cleanup**: `cleanupOrphanedSelfies(validPaths)` identifies and removes unreferenced image files from the private storage directory.
* **Re-enrollment Overwrite**: Enrolling a staff member automatically purges previous enrollment image samples to prevent storage bloat.

---

## 3. Location & Permission Privacy

* **Just-In-Time Permissions**: Permissions (`CAMERA`, `ACCESS_FINE_LOCATION`) are only requested at the moment the user initiates enrollment or attendance.
* **Location Scope**: GPS coordinates are requested strictly on-demand when the user presses **Verify Attendance**—the app does not run background location tracking or geofences.
* **Accuracy Tagging**: The horizontal accuracy radius (meters) is persisted with every attendance record for audit compliance.

---

## 4. Threat Model & Countermeasures

| Threat Vector | Mitigation Implemented |
| :--- | :--- |
| **Tampered Attendance Log** | Room database with SQLite integrity checks; all fields validated before insert. |
| **Photo Spoofing (Presentation Attack)** | ML Kit head pose angle validation (rejects flat images angled off-axis); recommendation for future depth/liveness sensor. |
| **Race Condition / Double Tap** | `AtomicBoolean` concurrency lock debounces the trigger; exactly one transaction is processed. |
| **Stale Session Exploitation** | Logout immediately clears DataStore preferences and wipes the Compose navigation backstack. |
| **Credential Theft** | Default passwords required for hiring assignment; architecture is designed to support Argon2/PBKDF2 hashing in production. |
