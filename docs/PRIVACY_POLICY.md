# Privacy Policy for Warranty Wallet

**Last Updated:** October 2026

Warranty Wallet ("we", "our", or "the App") respects and prioritizes your personal privacy. This Privacy Policy details how your personal data, purchase records, and documents are handled when using our native Android mobile application.

---

### 1. The Core Principle: Offline-First & Private

Warranty Wallet is architected to operate fully **offline-first**. You are never forced to register or create an account to use the application.

- When used in **Guest Mode**, 100% of your product information, receipt photos, serial numbers, and reminders are stored exclusively within local storage on your device.
- We do not run third-party advertising SDKs, behavioral telemetry trackers, or ad brokers.

---

### 2. Information We Collect (When You Choose Cloud Backup)

If you voluntarily create an account or sign in for Cloud Sync:
- **Account Identity**: Your email address and a unique Firebase Authentication User ID.
- **Product Metadata**: Product names, brands, categories, purchase dates, warranty expiry dates, seller names, purchase prices, and optional notes.
- **Uploaded Documents**: Invoices and receipt images or PDF files you deliberately upload.
- **Diagnostics**: Anonymous crash reports (via Firebase Crashlytics) to fix defects and improve stability.

---

### 3. On-Device Bill Text Recognition (OCR)

Our invoice parsing feature uses **Google ML Kit on-device text recognition**.
- When you scan a bill or attach an image, character recognition runs locally on your device's processor.
- Your bill image is not uploaded to third-party AI APIs for extraction.

---

### 4. Permissions Requested & Purpose

- **Camera (`android.permission.CAMERA`)**: Used solely when you tap "Scan Bill" to photograph a physical receipt. Never accessed in the background.
- **Notifications (`android.permission.POST_NOTIFICATIONS`)**: Used solely to alert you 30 days, 7 days, and 1 day prior to warranty expiration.
- **Exact Alarms (`android.permission.SCHEDULE_EXACT_ALARM`)**: Used to schedule accurate local reminders on your device's system clock.
- **Internet (`android.permission.INTERNET`)**: Used only for optional cloud backup, Firestore synchronization, and account management.

---

### 5. Data Security & Storage Rules

All cloud interactions use encrypted HTTPS/TLS protocols.
Cloud data is guarded by strict, per-user security rules:
- Only authenticated users can access their own subcollection (`users/{userId}/*`).
- File sizes for receipt storage are limited to 10MB per document, restricted to image and PDF MIME types.

---

### 6. Data Portability & Account Deletion

Under global data protection standards (including GDPR, CCPA, and Indian DPDP principles):
- **Data Export**: You can export your full database to **JSON or CSV** at any time directly from the Settings screen.
- **Local Data Deletion**: You can tap "Clear Local Data" to immediately wipe cached records on your phone.
- **In-App Account Deletion**: You can tap "Delete Account" in Settings to permanently delete your Firebase user account and cloud storage vault.

---

### 7. Contact & Support

For privacy inquiries, questions, or data requests, contact our support team at:
`support@warrantywallet.app`
