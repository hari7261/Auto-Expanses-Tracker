# 🛡️ Private Expenses

> **A 100% offline, privacy-first personal financial ledger with intelligent on-device payment notification and SMS parsing.**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Storage-Room%20(SQLite)-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Privacy Guarantee](https://img.shields.io/badge/Privacy-100%25%20Offline%20(Zero%20Internet%20Permission)-00C853?style=for-the-badge&logo=shield&logoColor=white)](#-is-this-secure-security--privacy-deep-dive)

---

```
  ┌────────────────────────────────────────────────────────────────────────┐
  │                                                                        │
  │     ██████╗ ██████╗ ██╗██╗   ██╗ █████╗ ████████╗███████╗              │
  │     ██╔══██╗██╔══██╗██║██║   ██║██╔══██╗╚══██╔══╝██╔════╝              │
  │     ██████╔╝██████╔╝██║██║   ██║███████║   ██║   █████╗                │
  │     ██╔═══╝ ██╔══██╗██║╚██╗ ██╔╝██╔══██║   ██║   ██╔══╝                │
  │     ██║     ██║  ██║██║ ╚████╔╝ ██║  ██║   ██║   ███████╗              │
  │     ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═══╝  ╚═╝  ╚═╝   ╚═╝   ╚══════╝              │
  │                                                                        │
  │              E X P E N S E S   •   L E D G E R                         │
  │                                                                        │
  │      [🔒 Air-Gapped]  [⚡ On-Device AI/Regex]  [📊 Rich Insights]      │
  │                                                                        │
  └────────────────────────────────────────────────────────────────────────┘
```

---

## 📖 Overview

**Private Expenses** is designed around a single uncompromising principle: **your financial data belongs strictly to you.**

Most modern expense tracking apps require account creation, synchronize bank credentials to third-party cloud servers, read your entire SMS inbox for remote scraping, and monetize your purchasing habits through targeted advertisements. 

**Private Expenses flips the paradigm:**
- **Zero Cloud**: Operates entirely air-gapped on your physical device.
- **Zero Internet Permission**: The app doesn’t even request `android.permission.INTERNET`. It is physically incapable of transmitting your financial records anywhere.
- **Intelligent On-Device Detection**: Automatically detects transaction debits and credits from payment app notifications (Google Pay, PhonePe, Paytm, CRED, BHIM, etc.) and bank SMS in real-time using an on-device regex parsing engine.
- **Strict User Sovereignty**: Alerts are placed in a staging queue for your approval. No transaction is ever logged without your review and consent.
- **Clean Material 3 Design**: Crafted with Jetpack Compose and Material 3, featuring fluid dark/light theming, high-contrast accessible typography, and interactive financial charts.

---

## 🔒 Is This Secure? (Security & Privacy Deep Dive)

Security is not a marketing promise in Private Expenses—it is architecturally enforced at the operating system level:

### 1. Air-Gapped by Design (No `INTERNET` Permission)
Check `app/src/main/AndroidManifest.xml` yourself: **there is no `android.permission.INTERNET` declaration.**
Because the app does not hold the Android network permission, the Android OS blocks the application process from opening sockets, making HTTP/REST requests, or transmitting telemetry to any external server. Your transactions cannot leak, even if compromised dependencies existed.

### 2. Sandboxed Local Room Database
All expense records, budget ceilings, and user preferences reside in a local SQLite database powered by Android Jetpack Room. This database is stored inside the app’s private internal storage sandbox (`/data/user/0/...`), inaccessible to other apps installed on the device.

### 3. Biometric & Security PIN Gate
When enabled in Settings, the entire ledger is protected by hardware-backed biometrics (Fingerprint / Face Unlock) or an encrypted 4-digit security PIN screen. App content remains hidden until verified.

### 4. Zero Silent Auto-Imports
When a payment notification or bank SMS is detected, Private Expenses **never** injects it directly into your live financial history. Instead:
1. It parses the amount, merchant, and payment mode in memory.
2. It generates a temporary entry in the **Alerts / Suggestions** staging screen.
3. You retain full control to edit the category, change the note, adjust the amount, approve the record, or dismiss it permanently.

### 5. Configurable App Allowlist
You choose exactly which payment apps Private Expenses is allowed to monitor. You can toggle Google Pay, Paytm, PhonePe, CRED, Amazon Pay, or add any custom package name at will.

### 6. Transparent Data Portability
You own your data completely. At any time, you can:
- Export your filtered transactions to standard **CSV** format for Excel, Google Sheets, or tax accounting.
- Create an encrypted **JSON backup** to transfer to a new device via the Android system share sheet.

---

## ✨ Features

| Screen / Feature | Description |
| :--- | :--- |
| **🏠 Dashboard** | High-level monthly spending summary, today's total spending, remaining budget status with dynamic progress indicator, top active categories, and recent transactions. |
| **⚡ Alerts & Suggestions** | Real-time staging queue for captured bank SMS and notification payments. Approve with one tap, edit details before saving, or swipe to ignore. |
| **🛠️ Payment Simulator** | Built-in test center to simulate real-world bank transactions (e.g., Swiggy, Amazon, Uber, Salary credit) without needing physical cards. |
| **📜 Transaction History** | Search transactions by merchant name or notes, filter by **All / Debits / Credits**, and sort chronologically or by amount. |
| **🎯 Budgets & Limits** | Configure an overall monthly spending ceiling as well as category-specific monthly caps. Visual warning bars notify you when you approach or exceed limits. |
| **📊 Insights & Analytics** | Interactive charts (Weekly, Monthly, Yearly, Custom date range), net balance cash flow, category distribution donut ring, and daily average spending. |
| **🔐 Biometric App Lock** | Optional security gate requiring biometric fingerprint/face authentication or a 4-digit PIN code upon opening the app. |
| **🎨 Theming & Appearance** | Seamless toggle between **Obsidian Dark**, **Crisp Light**, or **System Default** themes, built with Material 3 dynamic color tokens. |
| **📤 Data Export & Backup** | Export clean CSV files with custom date filters and backup/restore full database records safely without cloud storage. |

---

## 📱 How to Use Private Expenses

### Step 1: First Launch & Quick Setup
1. Launch the app. You will be greeted by the Apple-inspired splash animation.
2. If you would like biometric security, navigate to **Settings** (`⚙️`) and enable **Biometric & PIN Lock**.
3. Choose your preferred appearance: **Dark**, **Light**, or **System default**.

### Step 2: Adding Expenses Manually
1. On the **Dashboard** or **Transactions** screen, tap the floating `+` button.
2. Enter the amount, choose whether it is an **Expense (Debit)** or **Income (Credit)**.
3. Select a category (e.g., *Food, Shopping, Bills, Travel, Groceries, Medical, Entertainment, Other*).
4. Enter the merchant name (e.g., "Starbucks") and select the payment mode (*UPI, Credit Card, Debit Card, Cash, Net Banking*).
5. Tap **Save Expense**.

### Step 3: Enabling Automatic Bank SMS & Notification Parsing
1. Go to the **Alerts** tab (`💬`) or **Settings** (`⚙️`).
2. Tap **Grant Notification Listener Access** and toggle Private Expenses to **ON** in Android Settings.
3. If you want direct bank SMS tracking, allow the SMS permission prompt.
4. From now on, whenever you pay using Google Pay, PhonePe, Paytm, or receive an official bank SMS (HDFC, SBI, ICICI, etc.), Private Expenses will extract the details automatically.

### Step 4: Testing with the Payment Simulator (No Real Money Needed)
1. Navigate to the **Alerts** tab (`💬`).
2. Tap the lightning bolt icon (`⚡`) in the top bar to open the **Simulate Payment Alert** modal.
3. Tap any sample preset (e.g., *"Swiggy ₹450"*, *"Amazon ₹1,899"*, *"Zomato ₹320"*, or *"Salary Credit ₹75,000"*).
4. Watch the alert appear instantly in your suggestions queue! Tap **Quick Save** or **Edit Details** to test the complete workflow.

### Step 5: Managing Budgets
1. Navigate to the **Budget** tab (`🏛️`).
2. Set your **Overall Monthly Budget** ceiling (e.g., ₹30,000).
3. Tap **Add Category Budget** to set specific ceilings for *Food*, *Shopping*, etc.
4. Watch your progress bars update in real-time as expenses are approved.

### Step 6: Reviewing Analytics & Exporting CSV
1. Tap **Insights** (`📈`) from the bottom navigation bar.
2. Switch between **Weekly**, **Monthly**, **Yearly**, or **Custom Date Range**.
3. Inspect your net cash flow, bar charts, category donut ring, and average daily burn rate.
4. Tap the **Export CSV** button in the top bar to share or save your spreadsheet.

---

## 🏗️ Technical Architecture

Private Expenses is developed using the latest modern Android standards:

```
app/src/main/java/com/example/
├── MainActivity.kt                      # Main Activity, Root NavHost, Splash & PIN Gate
├── PrivateExpensesApplication.kt        # Application class initializing Room DB & App Context
├── data/
│   └── local/
│       ├── ExpenseDatabase.kt           # Room Database with migration strategies
│       ├── dao/
│       │   └── ExpenseDao.kt            # Reactive Flow-based database queries
│       └── entities/
│           ├── Expense.kt               # Core expense record entity
│           ├── Budget.kt                # Monthly category budget entity
│           └── NotificationSuggestion.kt# Captured alerts staging entity
├── presentation/
│   ├── viewmodel/
│   │   └── ExpenseViewModel.kt          # Central state management with StateFlow
│   └── screens/
│       ├── DashboardScreen.kt           # Home dashboard with clean M3 budget status card
│       ├── TransactionsScreen.kt        # Searchable transaction history with filters
│       ├── SuggestionsScreen.kt         # Pending alert queue & Payment Simulator
│       ├── BudgetsScreen.kt             # Monthly budget limits & warning alerts
│       ├── InsightsScreen.kt            # Charts, analytics & CSV export engine
│       ├── SettingsScreen.kt            # Privacy allowlist, PIN lock & theme prefs
│       └── AddExpenseDialog.kt          # Unified transaction creation/editing dialog
├── service/
│   ├── PaymentNotificationListenerService.kt # Android NotificationListenerService
│   ├── SmsReceiver.kt                   # BroadcastReceiver for bank SMS debits
│   └── ParserEngine.kt                  # Regex extraction engine for transaction data
└── ui/
    └── theme/
        ├── Color.kt                     # Curated M3 palette (Obsidian, Emerald, Violet)
        ├── Theme.kt                     # Dynamic theme provider with dark/light modes
        └── Type.kt                      # Modern typographic scale
```

---

## 📋 Permission Breakdown

| Permission | Android Declaration | Justification & Scope |
| :--- | :--- | :--- |
| **Notification Listener** | `BIND_NOTIFICATION_LISTENER_SERVICE` | Required solely to read incoming push receipts from allowed payment apps. Zero notification data ever leaves your device. |
| **Receive SMS** | `android.permission.RECEIVE_SMS` | Captures bank debit/credit confirmation SMS in real-time for instant budget updating. |
| **Read SMS** | `android.permission.READ_SMS` | Allows one-time scanning of recent bank confirmation SMS upon user request. |
| **Post Notifications** | `android.permission.POST_NOTIFICATIONS` | Displays a local reminder when a high-value expense alert is ready for your review. |
| **INTERNET** | ❌ **NOT REQUESTED** | **Intentionally omitted.** The app cannot access the internet under any circumstances. |

---

## 🛠️ Building & Testing

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- Android SDK 35 (compileSdk: 35, minSdk: 26)
- Java 17 / OpenJDK 17

### Build Commands
To assemble a debug APK:
```bash
gradle assembleDebug
```

To run local Robolectric and JUnit unit tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 📄 License

This application is proprietary and designed for private personal finance management. You are free to modify, build, and use this application on your personal Android devices with complete peace of mind.

**Private Expenses: Because your financial life is nobody else's business.**
