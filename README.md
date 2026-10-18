# Warranty Wallet (Android)

> **Keep every bill. Remember every warranty. Never miss a service deadline.**

Warranty Wallet is a modern, production-grade native Android application that empowers everyday users to organize purchase receipts, automatically parse invoice details via on-device machine learning, track warranty timelines, and receive proactive expiry alerts before free service windows close.

---

## 🚀 Key Highlights & Architecture

- **Platform**: Native Android (100% Kotlin)
- **UI Framework**: Jetpack Compose with Material 3 Design System
- **Architecture**: MVVM + Clean Architecture + Unidirectional Data Flow (UDF)
- **Local Persistence (Offline-First)**: Room Database (`WarrantyWalletDatabase`) with reactive Flow streams
- **Preferences**: Jetpack DataStore Preferences (App Theme, Language, Onboarding, Guest Mode)
- **OCR Engine**: Google ML Kit On-Device Text Recognition (zero third-party data transmission)
- **Background Notifications**: Exact and Inexact local alarms via `AlarmManager` with `NotificationCompat`, resilient across device reboots via `BootCompletedReceiver`
- **Cloud Backend**: Firebase Authentication, Cloud Firestore, Firebase Cloud Storage
- **Security**: Granular Firestore Security Rules and Storage Rules restricting access to authenticated resource owners (`users/{userId}/*`)
- **Localization**: English, Hindi (हिंदी), and Tamil (தமிழ்)
- **Data Portability**: Full JSON and CSV personal data export & in-app account deletion

---

## 📂 Project Directory Structure

```
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/warrantywallet/app/
│   │   │   │   ├── core/
│   │   │   │   │   ├── database/       # Room database, Entities, DAOs, Type Converters
│   │   │   │   │   ├── datastore/      # DataStore PreferenceManager
│   │   │   │   │   ├── designsystem/   # Color palette, Material 3 Theme, Typography, Reusable UI Components
│   │   │   │   │   ├── model/          # Domain Models & Enums (WarrantyStatus, Category, Product, etc.)
│   │   │   │   │   ├── notification/   # NotificationHelper, AlarmScheduler, Broadcast Receivers
│   │   │   │   │   └── util/           # DateUtils, BillOcrParser, ExportUtils
│   │   │   │   ├── data/
│   │   │   │   │   ├── remote/         # FirestoreSyncService
│   │   │   │   │   └── repository/     # ProductRepository, AuthRepository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── navigation/     # NavHost, Routes, BottomNavigationBar
│   │   │   │   │   └── screens/        # Onboarding, Auth, Home, Products, Detail, AddEdit, Reminders, Documents, Settings, Search
│   │   │   │   ├── MainActivity.kt     # AndroidX SplashScreen + Edge-to-Edge Host
│   │   │   │   └── WarrantyWalletApp.kt # Application container & Notification channel setup
│   │   │   ├── res/                    # Adaptive Vector Icons, Strings (en, hi, ta), Themes, Colors
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Comprehensive Unit Test Suite (Date calculations, OCR heuristics, Export formatters)
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── proguard-rules.pro
├── docs/                               # Play Store listing, Privacy Policy, Terms, Data Safety Guide
├── firestore.rules                     # Cloud Firestore per-user authorization rules
├── storage.rules                       # Cloud Storage file size & MIME validation rules
├── firestore.indexes.json              # Composite index declarations
└── firebase.json                       # Firebase deployment & emulator configuration
```

---

## 🛠️ Build and Run Instructions

### Prerequisites
1. **JDK 17**: Oracle JDK 17, Eclipse Temurin 17, or Android Studio's bundled JBR 17.
2. **Android SDK**: Compile SDK 35, Min SDK 26.
3. **Gradle**: 8.10.2 (configured in wrapper).

### Environment Setup (Windows PowerShell)
```powershell
# Set JDK 17 home (adjust path if needed)
Set-Item env:JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"
Set-Item env:ANDROID_HOME "C:\Android\Sdk"
```

### Run Unit Tests
```powershell
.\gradlew.bat test
```

### Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```
Output location: `app/build/outputs/apk/debug/app-debug.apk`

### Build Production Signed App Bundle (AAB)
```powershell
.\gradlew.bat bundleRelease
```
Output location: `app/build/outputs/bundle/release/app-release.aab`

---

## 🔥 Firebase & Security Deployment

To deploy Firestore security rules and Cloud Storage rules:
```bash
firebase login
firebase deploy --only firestore:rules,storage
```

---

## 📄 License & Attribution
Developed for the **Warranty Wallet** product launch.
All rights reserved.
