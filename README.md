# 📱 SMS Expense Tracker

A personal finance Android app that **automatically reads your bank SMS messages** and turns them into a clean, organized expense tracker — no manual entry needed.

---

## 📌 What Does This App Do?

Every time your bank sends you an SMS like:

> *"Your a/c no. XX0046 debited for Rs.34.00 on 17-03-2026 19:17:24 trf to bharatpe@upi (RefNo 120174243512) - DNS Bank"*

This app:
1. **Reads** that SMS automatically
2. **Extracts** the amount, date, type (credit/debit), and party
3. **Saves** it to a local database on your phone
4. **Displays** it in a clean list — latest transaction on top

You can also **add cash transactions manually** using the + button.

---

## 🛠️ Technologies Used

| Technology | What It Is | Why We Used It |
|------------|-----------|----------------|
| **Java** | Programming language | Main language for Android development |
| **Android Studio** | Development tool | Official IDE for building Android apps |
| **SQLite** | Local database | Stores all transactions on the phone — no internet needed |
| **RecyclerView** | UI component | Shows transactions in a smooth scrollable list |
| **XML Layouts** | UI design files | Defines how every screen looks |
| **ContentResolver** | Android API | Used to read SMS messages from the phone |
| **Regex** | Pattern matching | Extracts amount, date, party from SMS text |
| **Google AdMob** | Ad network | Shows a banner ad at the bottom of the screen |
| **AsyncTask** | Background processing | Scans SMS without freezing the app screen |
| **SharedPreferences** | Small data storage | Remembers if the app has been opened before |

---

## 📂 Project Structure

```
app/src/main/
│
├── java/com/example/smsexpensetracker/
│   │
│   ├── MainActivity.java          → Main screen — controls everything
│   ├── Transaction.java           → Data model (like a blueprint for one transaction)
│   ├── DatabaseHelper.java        → All database operations (save, read, update)
│   ├── SMSReader.java             → Reads SMS inbox from the phone
│   ├── SMSParser.java             → Extracts data from SMS text using Regex
│   ├── TransactionAdapter.java    → Connects transaction data to the list on screen
│   ├── TransactionDialog.java     → Popup shown when you tap a transaction
│   └── AddTransactionDialog.java  → Popup for manually adding a cash transaction
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml          → Main screen layout
│   │   ├── item_transaction.xml       → Single row in the list
│   │   ├── dialog_transaction.xml     → Transaction detail popup layout
│   │   └── dialog_add_transaction.xml → Add cash transaction popup layout
│   │
│   ├── values/
│   │   ├── colors.xml    → App color definitions
│   │   ├── strings.xml   → All text used in the app
│   │   └── themes.xml    → App theme and style
│   │
│   └── drawable/
│       └── bg_dialog_rounded.xml  → Rounded white background for popups
│
└── AndroidManifest.xml  → App permissions and configuration
```

---

## 🔄 How The App Works — Step By Step

### First Time You Open The App

```
App Opens
    ↓
Ask for SMS Permission
    ↓
Permission Granted?
    ├── YES → Scan all SMS in inbox
    └── NO  → Show empty screen (can grant later)
            ↓
    Read all SMS from inbox
            ↓
    For each SMS — is it a bank SMS?
    ├── NO  → Skip it
    └── YES → Extract details using Regex
                    ↓
            Amount found?
            ├── NO  → Skip
            └── YES → Save to SQLite database
                            ↓
                    Show in list (latest on top)
```

### Every Time After That

```
App Opens
    ↓
Load transactions from SQLite database
    ↓
Show in RecyclerView list
```

### When You Tap Reload Button (🔄)

```
Tap Reload FAB
    ↓
Scan SMS inbox again
    ↓
Only NEW SMS are added (duplicates are automatically skipped)
    ↓
List refreshes
```

### When You Tap + Button

```
Tap Add FAB
    ↓
Popup opens
    ↓
Fill in: Date, Time, Credit/Debit, Amount, Party, Description
    ↓
Tap Add
    ↓
Saved to database
    ↓
Appears at top of list
```

### When You Tap A Transaction Row

```
Tap any row
    ↓
Popup opens showing full details:
  • Date & Time
  • Amount
  • Type (Credited / Debited)
  • Party (who sent/received)
  • Reference number
  • Description (editable)
    ↓
Edit description if needed → Tap Save
    ↓
Updated in database instantly
```

---

## 🧠 How SMS Parsing Works (The Smart Part)

The app uses **Regex (Regular Expressions)** — a way to find patterns in text.

### Example SMS:
```
Your a/c no. XX0046 debited for Rs.34.00 on 17-03-2026 19:17:24
trf to bharatpe.9q0p0e0k0c835361@unit (RefNo 120174243512). DNS Bank
```

### What Gets Extracted:

| Field | Extracted Value | How |
|-------|----------------|-----|
| Amount | 34.00 | Finds pattern after "Rs." |
| Type | Debited | Finds the word "debited" |
| Date-Time | 17-03-2026 19:17:24 | Finds dd-MM-yyyy HH:mm:ss pattern |
| Party | bharatpe.9q0p0e0k0c835361@unit | Finds VPA (word@word) pattern |
| Reference | 120174243512 | Finds number after "RefNo" |

### Supported Banks & Apps

| Bank / App | Supported |
|-----------|-----------|
| DNS Bank | ✅ |
| HDFC Bank | ✅ |
| SBI | ✅ |
| ICICI Bank | ✅ |
| Axis Bank | ✅ |
| Kotak Bank | ✅ |
| GPay | ✅ |
| PhonePe | ✅ |
| Paytm | ✅ |
| BHIM UPI | ✅ |
| Most Indian banks | ✅ |

---

## 🗄️ Database Structure

The app stores everything in a single SQLite table called **transactions**:

| Column | Type | What It Stores |
|--------|------|----------------|
| id | Number | Auto-generated unique ID |
| datetime | Text | Date and time of transaction |
| amount | Number | Transaction amount in Rs. |
| type | Text | "Credited" or "Debited" |
| description | Text | User-editable note |
| party | Text | Who sent/received the money |
| reference | Text | Bank reference / UTR number |
| sms_id | Text | Original SMS ID (prevents duplicates) |
| sms_date | Number | SMS timestamp (used for sorting) |

> **Duplicate Prevention:** Every SMS has a unique `sms_id`. If the same SMS is scanned twice, the database automatically ignores it.

---

## 📱 App Screens

### Main Screen
- Blue toolbar with app name
- Summary bar showing: total transactions, total credited, total debited
- Column headers: Date/Time | Amount | Type
- Scrollable list of all transactions
  - 🟢 Green = Credited (money received)
  - 🔴 Red = Debited (money spent)
- 🔄 Reload FAB button (blue) — re-scans SMS
- ➕ Add FAB button (orange) — add cash transaction
- AdMob banner at the very bottom

### Transaction Detail Popup
- Full details of the transaction
- Editable description field
- Save and Close buttons

### Add Transaction Popup
- Date picker
- Time picker
- Credit / Debit radio buttons
- Amount field
- Party/Person field
- Description field
- Add and Cancel buttons

---

## 🔐 Permissions

| Permission | Why Needed |
|-----------|-----------|
| READ_SMS | To read bank SMS messages from inbox |
| INTERNET | Required for AdMob to show ads |
| ACCESS_NETWORK_STATE | Required by AdMob SDK |

> **Privacy:** All data stays **on your phone**. No data is sent to any server. The app works completely offline (except for AdMob ads).

---

## 💰 AdMob Integration

- Banner ad displayed at the bottom of the main screen
- **App ID:** ca-app-pub-4195105056058261~4846325772
- **Ad Unit ID:** ca-app-pub-4195105056058261/4225843823
- Ad lifecycle is properly managed (paused/resumed/destroyed with Activity)

---

## ⚙️ Setup Instructions

### Requirements
- Android Studio (latest version)
- Android phone with Android 5.0 or above
- Physical device recommended (SMS not available on emulator)

### Steps
1. Open Android Studio
2. **File → Open** → select the `SMSExpenseTracker` folder
3. Wait for Gradle sync to complete
4. Connect your Android phone via USB
5. Click **Run ▶**
6. Grant SMS permission when asked
7. App automatically scans your SMS inbox

### Building an APK to share
1. **Build → Build Bundle(s)/APK(s) → Build APK(s)**
2. APK is saved at: `app/build/outputs/apk/debug/app-debug.apk`
3. Share via WhatsApp or Google Drive
4. Recipients may see a Play Protect warning — tap **Install Anyway**

---

## 🐛 Common Issues & Fixes

| Problem | Fix |
|---------|-----|
| App crashes on open | Grant SMS permission in phone Settings |
| No transactions showing | Tap the 🔄 reload button |
| Wrong transactions showing | App only reads bank SMS — check SMSParser keywords |
| Banner ad not showing | New AdMob accounts take 24-48 hours to activate |
| "App not installed" error | Uninstall old version first then reinstall |
| Play Protect warning | Tap OK → Install Anyway — this is normal for non-Play Store apps |

---

## 📊 App Stats (Example)

Once running with real data you will see something like:

```
899 Transactions  |  + Rs.115387  |  - Rs.97226
```

This means:
- 899 total bank transactions found
- Rs.1,15,387 total money received
- Rs.97,226 total money spent

---

## 👨‍💻 Built With

- **Language:** Java (100%)
- **Database:** SQLite (local, on-device)
- **UI:** Material Design Components
- **Ads:** Google AdMob SDK 23.0.0
- **Min Android:** 5.0 (API 21)
- **Target Android:** 14 (API 34)
- **Build Tool:** Gradle 8.3.0

---

*Built for personal finance tracking. All data stored locally on device. No cloud, no login, no subscription.*
