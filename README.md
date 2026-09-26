# Android Attendance App

A modern, production-grade, offline-first Android Attendance Application featuring **real on-device facial recognition** and **GPS location tagging**, built with Jetpack Compose, CameraX, Google ML Kit, and TensorFlow Lite.

Developed as a hiring assignment for **SalaryBox**.

---

## 1. Overview

The SalaryBox Attendance App provides a complete, dual-role attendance management system:

* **Administrator**: Manages staff members, tracks organization-wide enrollment and attendance activity, and enrolls employee biometric face templates using the front camera.
* **Staff Member**: Authenticates securely, views personal attendance history, and marks daily attendance verified against their enrolled facial identity and device GPS location.

The entire biometric pipeline—face detection, pose validation, facial crop/alignment, neural network embedding generation, and cosine similarity comparison—runs **100% on-device** without any cloud dependencies.

---

## 2. Features

* **Dual-Role Authentication**: Dedicated Admin and Staff workflows with persistent session storage (Jetpack DataStore Preferences).
* **Real On-Device Biometric Verification**: MobileFaceNet neural network (192-dimensional embeddings) running via TensorFlow Lite.
* **Intelligent Face Detection & Quality Validation**: Google ML Kit detects face count, validates head pose (Yaw, Pitch, Roll $\le 20^\circ$), and enforces minimum face size before inference.
* **GPS Location Tagging**: High-accuracy location capture using Google Play Services `FusedLocationProviderClient` with a strict 10-second timeout and offline resilience.
* **App-Private Secure Storage**: Biometric image assets and selfies are stored in app-internal sandboxed storage (`context.filesDir`), never committed to version control or exposed to other apps.
* **Concurrency & Duplicate Protection**: Atomic debouncing prevents accidental double-tap attendance submissions.
* **Modern Jetpack Compose UI**: Built with Material 3 design system, supporting edge-to-edge layout, responsive cards, and dynamic feedback.

---

## 3. Architecture

The application adopts an **offline-first Clean Architecture** pattern following Unidirectional Data Flow (UDF):

```
┌────────────────────────────────────────────────────────┐
│           Jetpack Compose UI (Material 3)              │
│       Screens, State Collection, CameraX Previews      │
└───────────────────────────▲────────────────────────────┘
                            │ State / Events
┌───────────────────────────┴────────────────────────────┐
│              Lifecycle-Aware ViewModels                │
│            StateFlow & Concurrency Guards              │
└───────────────────────────▲────────────────────────────┘
                            │ Domain Calls
┌───────────────────────────┴────────────────────────────┐
│                Domain & Repository Layer               │
│        StaffRepository, AttendanceRepository, Auth     │
└───────▲───────────────────▲────────────────────▲───────┘
        │                   │                    │
┌───────┴──────┐    ┌───────┴────────┐   ┌───────┴───────┐
│ Room SQLite  │    │ Face Engine    │   │ Location      │
│ Database     │    │ ML Kit + TFLite│   │ FusedLocation │
└──────────────┘    └────────────────┘   └───────────────┘
```

---

## 4. Technology Stack

* **Language**: Kotlin 2.0.20
* **UI Framework**: Jetpack Compose with Material 3 (BOM 2024.09.00)
* **Architecture Components**: ViewModel, Navigation Compose 2.7.7, StateFlow, Coroutines
* **Local Persistence**: Room SQLite 2.6.1 with KSP (Kotlin Symbol Processing)
* **Session Management**: Jetpack DataStore Preferences 1.1.1
* **Camera Pipeline**: CameraX 1.3.4 (PreviewView, ImageCapture, ImageAnalysis)
* **Face Detection**: Google ML Kit Vision Face Detection 16.1.7
* **Neural Network Inference**: TensorFlow Lite 2.14.0 + TFLite Support 0.4.4
* **Location**: Google Play Services Location 21.3.0 (`FusedLocationProviderClient`)
* **Build System**: Android Gradle Plugin 8.5.1, Gradle 8.7, targetSdk 34, minSdk 26

---

## 5. Face Verification Pipeline

```
CameraX Front Capture
       │
       ▼
Google ML Kit Face Detection
  ├── Check: Exactly 1 face? (Reject: NO_FACE / MULTIPLE_FACES)
  ├── Check: Head pose (Yaw, Pitch, Roll <= 20°)? (Reject: POOR_QUALITY)
  └── Check: Face size >= 100x100px? (Reject: POOR_QUALITY)
       │
       ▼
Face Crop & Alignment (+15% padding, resized to 112x112 ARGB_8888)
       │
       ▼
Pixel Normalization: (pixel - 127.5) / 128.0 (maps [0, 255] to [-1.0, 1.0])
       │
       ▼
MobileFaceNet TFLite Model (192-dimensional output vector)
       │
       ▼
L2 Vector Normalization: v / max(||v||, 1e-10)
       │
       ▼
Cosine Similarity Comparison: dot_product(candidate, enrolled_template)
       │
       ├── Score >= 0.70 ──> MATCH (Verified)
       └── Score < 0.70  ──> LOW_SIMILARITY (Rejected)
```

> **Crucial Distinction**: Face detection is **not** identity recognition. Face detection locates *where* a face is in a 2D image. Identity recognition computes a high-dimensional mathematical embedding and compares it against an enrolled biometric identity template.

---

## 6. Data Storage

* **Relational Data**: SQLite database managed via Room (`data/database/AppDatabase.kt`):
  * `staff`: Employee ID, name, active flag, timestamps.
  * `face_enrollment`: Foreign key to `staff`, serialized 192-d normalized embedding, model version, sample count.
  * `attendance`: Foreign key to `staff`, epoch timestamp, selfie file path, latitude, longitude, horizontal accuracy, match score, verification status.
* **Biometric Images**: Selfies and enrollment images are saved strictly in `context.filesDir/selfies/` and `context.filesDir/enrollments/`. Files are referenced by path in Room and cleaned up automatically upon re-enrollment or deletion.

---

## 7. Permissions

* `android.permission.CAMERA`: Required for front-camera face enrollment and live attendance capture.
* `android.permission.ACCESS_FINE_LOCATION`: Required for high-accuracy GPS coordinates during attendance tagging.
* `android.permission.ACCESS_COARSE_LOCATION`: Fallback network location support.

*All permissions are requested just-in-time when initiating a camera or attendance action.*

---

## 8. Demo Credentials

| Role | Identifier | Password | Initial State |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | System administrator account |
| **Staff** | `EMP001` | `1234` | Seeded employee (*Demo Employee*), face not enrolled |

---

## 9. Demo Flow

1. **Admin Login**: Log in as `admin` / `admin123`.
2. **Staff List**: Open **Manage Staff**, select **Demo Employee (EMP001)**.
3. **Face Enrollment**: Tap **Enroll Face**, position face in the oval guide, capture 3 samples.
4. **Staff Login**: Log out and log in as `EMP001` / `1234`.
5. **Mark Attendance**: Tap **Mark Attendance**, face the camera, tap **Verify & Mark Attendance**.
6. **Verification & Location**: Engine verifies identity (confidence score displayed) and tags GPS coordinates.
7. **Attendance History**: View past attendance logs with exact timestamps and verification statuses.
8. **Negative Test**: Have a different person attempt attendance on `EMP001` $\implies$ rejected with *"Face does not match your enrolled profile."*

---

## 10. Setup & Build

### Prerequisites
* JDK 17 or JDK 21 installed and configured in `JAVA_HOME`.
* Android SDK platforms 33/34 installed (`ANDROID_HOME`).

### Build APKs
```powershell
# Clone repository
git clone https://github.com/04lxshyak/SalaryBox_Assignment.git
cd SalaryBox_Assignment

# Run automated tests
.\gradlew.bat testDebugUnitTest

# Build debug APK
.\gradlew.bat assembleDebug

# Build signed release APK
.\gradlew.bat assembleRelease
```

Generated APKs:
* **Historical APK**: `release/attendance-app.apk` (72.6 MB). Its relationship to the current source has not been verified; generate a fresh APK before submission.
* **Debug APK output after a successful build**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 11. Testing

The automated test suite verifies:
* **Attendance Policy Gates A–I** (`AttendanceAuditScenariosTest.kt`): Happy path, wrong face, no location, no face, multiple faces, missing enrollment, denied permissions, double-tap debounce.
* **Biometric Mathematics** (`FaceSimilarityTest.kt`): L2 normalization, cosine similarity bounds, threshold boundary testing, multi-template matching, Person A vs. Person B discrimination.
* **Authentication & Session** (`AuthRepositoryTest.kt`, `SessionManagerTest.kt`).
* **Data Integrity** (`StaffValidationTest.kt`, `EntityMappingTest.kt`).

Run tests via:
```powershell
.\gradlew.bat test
```

---

## 12. Assumptions & Limitations

1. **2D Camera Biometrics**: Uses single-lens RGB front camera. While head pose validation prevents simple flat photo presentations, production deployments require infrared depth sensors (TrueDepth/IR) for hardware-level anti-spoofing.
2. **Lighting Conditions**: Severe underexposure or extreme backlighting will cause ML Kit to fail closed (`NO_FACE`).
3. **Threshold Calibration**: The decision threshold (`0.70f`) was practically tuned for mobile office environments.
4. **No Remote Sync**: Per assignment specification, the application operates entirely offline with local database persistence.

---

## 13. Privacy & Security

* **100% On-Device Processing**: Biometric embeddings never leave the device.
* **No Biometric Logs**: Raw float arrays are never logged to logcat or console.
* **Fail Closed**: Any initialization failure, missing template, or low score aborts without writing an attendance record.
* **Sandboxed Storage**: Images reside in app-private storage inaccessible to other apps.

---

## 14. Project Structure

```
SalaryBox_Assignment/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── mobilefacenet.tflite     // Pre-trained MobileFaceNet model
│   │   ├── java/com/salarybox/attendance/
│   │   │   ├── camera/                 // CameraX preview & face oval guide overlay
│   │   │   ├── core/                   // Theme, Navigation (AppNavHost, NavRoute)
│   │   │   ├── data/                   // Room entities, DAOs, Database, DataStore, FileStorage
│   │   │   ├── di/                     // ServiceLocator dependency provider
│   │   │   ├── domain/                 // Models, Repository interfaces
│   │   │   ├── face/                   // RealFaceRecognitionEngine, Config, Results
│   │   │   ├── location/               // LocationService (FusedLocationProvider)
│   │   │   ├── presentation/           // ViewModels and Compose screens (Admin, Staff, Auth)
│   │   │   ├── AttendanceApp.kt        // Application startup & warmup
│   │   │   └── MainActivity.kt         // Root ComponentActivity
│   │   └── res/                        // Values, icons, themes
│   └── src/test/                       // Complete automated test suite
├── docs/                               // Architecture, Security, Traceability, Demo docs
├── release/
│   └── attendance-app.apk              // Historical APK; rebuild and verify before submission
├── README.md
├── build.gradle.kts
└── settings.gradle.kts
```
