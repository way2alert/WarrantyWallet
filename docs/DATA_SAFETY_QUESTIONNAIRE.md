# Google Play Data Safety Questionnaire Guide

Use these exact answers when filling out the Google Play Console Data Safety Form for Warranty Wallet:

### 1. Data Collection and Security
- **Does your app collect or share any of the required user data types?**  
  👉 **Yes** (Only when user creates an account for cloud backup; otherwise offline).
- **Is all of the user data collected by your app encrypted in transit?**  
  👉 **Yes** (All cloud network communication uses HTTPS / TLS 1.3).
- **Do you provide a way for users to request that their data be deleted?**  
  👉 **Yes** (Users can delete their account and all stored records directly in the app via Settings -> Delete Account).

---

### 2. Data Types Collected & Purpose

| Data Category | Data Type | Collected? | Shared? | Purpose | Optional or Required? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Personal Info** | Email address | Yes | No | Account management, Cloud sync | Optional (Guest mode requires no email) |
| **Personal Info** | User IDs | Yes | No | Firebase User ID for cloud backup | Optional |
| **Financial Info** | Purchase history (Price/Store) | Yes | No | App functionality (Tracking bill totals) | Optional |
| **Photos & Videos** | Photos (Receipts) | Yes | No | App functionality (Invoice preservation) | Optional |
| **Files & Docs** | Files (PDF Invoices) | Yes | No | App functionality (Invoice preservation) | Optional |
| **App Performance** | Crash logs (Crashlytics) | Yes | No | Analytics & bug fixing | Automatic |

---

### 3. Data Safety Summary for Users
- **No data shared with third parties** (No advertising networks, no data brokers).
- **Data encrypted in transit**.
- **User can request data deletion at any time**.
