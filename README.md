# SalaryBox Wallet

An Android wallet assignment built with Kotlin, Jetpack Compose, and Material 3. It provides a complete mock-wallet experience: authentication, provider discovery, wallet balance, add-money and send-money operations, transaction history, profile management, settings, and logout.

## Architecture

The application uses a clean MVVM structure under `com.salarybox.attendance.wallet`:

- `presentation`: Compose screens, Navigation Compose graph, and Hilt ViewModel
- `domain`: UI-ready models and input validation rules
- `data`: Room entities/DAO, Retrofit mock API, repository implementation, DataStore settings, and WorkManager sync
- `di`: Hilt bindings for the database, repository, OkHttp, and Retrofit

State is exposed with Kotlin `Flow` and collected with lifecycle awareness. Room is the source of truth for wallet balance, transactions, profile data, and cached providers. Retrofit reads public mock provider data from `https://jsonplaceholder.typicode.com/`; a WorkManager job refreshes that cache every 24 hours.

## Setup

1. Open the project in Android Studio Hedgehog or newer.
2. Use JDK 17 and install Android SDK Platform 34.
3. Sync Gradle, then run the `app` configuration on an API 26+ emulator or device.
4. Sign in with any syntactically valid email and a password containing at least four characters.

## Testing

Run unit tests with:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run lint with:

```powershell
.\gradlew.bat lintDebug
```

The project includes validation-focused unit coverage for login, amount, and recipient inputs. The primary UI elements expose test tags for login, home, add-money, and send-money workflows.

## Security Notes

- Network traffic is HTTPS-only and cleartext traffic is disabled.
- HTTP logging is disabled, avoiding sensitive data in logs.
- DataStore holds only non-sensitive UI/session flags; passwords are neither stored nor logged.
- Release builds enable R8/ProGuard minification.
- Amounts, recipient names, profiles, and login fields are validated before persistence.
- Android backups are disabled for the wallet application.

## Screens

Splash, login, home dashboard, provider list/detail, wallet, add money, send money, transaction history/detail, profile, settings, and logout are implemented.

## APK

After a successful release build, the APK is created at `app/build/outputs/apk/release/app-release-unsigned.apk`. A signed public-release asset must be produced in a trusted CI or Android Studio signing environment; no signing key is committed to this repository.
