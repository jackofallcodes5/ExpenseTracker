# SMS Expense Tracker – Android App

A complete Android application that reads bank transaction SMS messages, extracts financial data using Regex, stores them in SQLite, and displays them in a clean RecyclerView list.

---

## Project Structure

```
app/src/main/
├── java/com/example/smsexpensetracker/
│   ├── MainActivity.java          ← Entry point, permission handling, SMS import trigger
│   ├── SMSReader.java             ← Reads inbox via ContentResolver
│   ├── SMSParser.java             ← Regex extraction of amount, type, party, reference
│   ├── DatabaseHelper.java        ← SQLite CRUD (insert, read, update description)
│   ├── Transaction.java           ← Data model / POJO
│   ├── TransactionAdapter.java    ← RecyclerView adapter
│   └── TransactionDialog.java     ← Detail popup with editable description
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml      ← Main screen (toolbar, summary, header, RecyclerView, FAB)
│   │   ├── item_transaction.xml   ← Single row: DateTime | Amount | Type
│   │   └── dialog_transaction.xml ← Full detail popup with EditText for description
│   ├── drawable/
│   │   └── bg_dialog_rounded.xml  ← Rounded card background for dialog
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── themes.xml
│
└── AndroidManifest.xml            ← READ_SMS + RECEIVE_SMS permissions
```

---

## Setup Instructions

### 1. Open in Android Studio
- **File → Open** → select the `SMSExpenseTracker` folder
- Let Gradle sync complete

### 2. Build & Run
- Connect a physical Android device (SMS reading does **not** work on emulator)
- Click **Run ▶** or press `Shift+F10`

### 3. Permissions
- App requests `READ_SMS` at runtime on first launch
- Grant the permission → SMS scan begins automatically

---

## SQLite Schema

```sql
CREATE TABLE transactions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    datetime    TEXT,
    amount      REAL,
    type        TEXT,        -- 'Credited' | 'Debited' | 'Unknown'
    description TEXT,        -- User-editable
    party       TEXT,        -- VPA or merchant name
    reference   TEXT,        -- UTR / Ref number
    sms_id      TEXT UNIQUE  -- Prevents duplicate imports
);
```

---

## Supported SMS Formats

The Regex parser handles messages from most Indian banks:

| Bank | Format Example |
|------|---------------|
| HDFC | `Rs.500.00 credited to A/c XX1234 on 25-12-2024` |
| SBI  | `Your A/c XXXXXX1234 credited by INR 1,000.00 on 25/12/2024 Ref No 123456789` |
| ICICI | `ICICI Bank: Rs 2500 debited from A/c XX5678 on 25-Dec-2024; UPI Ref 123456789` |
| GPay / PhonePe | `Rs.300 sent to merchant@oksbi via UPI. UTR:412345678901` |
| Kotak | `INR 750.00 paid to user@paytm UPI Txn ID: 123456789012` |

### Extracted Fields
- **Amount** → `Rs.`, `Rs `, `INR`, `₹` followed by digits
- **Type** → `credited/received/deposited` → Credited; `debited/paid/spent` → Debited
- **DateTime** → `dd-MM-yyyy HH:mm`, `dd/MM/yyyy`, `dd-MMM-yyyy`
- **Party** → UPI VPA (`user@bank`) or `to/from <name>`
- **Reference** → `Ref No`, `UTR`, `Txn ID`, `Ref #`

---

## Features

| Feature | Details |
|---------|---------|
| First-launch auto-scan | Scans entire SMS inbox on first open |
| Duplicate prevention | `UNIQUE` constraint on `sms_id` |
| Background import | `AsyncTask` keeps UI responsive |
| Summary bar | Shows count + total credit/debit amounts |
| Color-coded rows | Green = Credited, Red = Debited |
| Detail dialog | Full info + editable description |
| Manual re-scan | FAB button for re-importing |
| Empty state | Friendly message when no transactions found |

---

## Notes

- **Physical device required** – Android emulators have no real SMS inbox
- `AsyncTask` is deprecated in API 30+; consider replacing with `ExecutorService + Handler` for production
- The `RECEIVE_SMS` permission is declared for future real-time SMS listening (BroadcastReceiver placeholder in manifest)
- minSdk 21 (Android 5.0) ensures very broad device compatibility

