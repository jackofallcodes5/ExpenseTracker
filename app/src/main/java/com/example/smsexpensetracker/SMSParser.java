package com.example.smsexpensetracker;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMSParser.java
 * Parses Indian bank SMS messages using Regex.
 * Covers most Indian banks: HDFC, SBI, ICICI, Axis, Kotak,
 * DNS Bank, GPay, PhonePe, Paytm, BHIM and more.
 */
public class SMSParser {

    private static final String TAG = "SMSParser";

    // -------------------------------------------------------
    // Banking keyword detection
    // -------------------------------------------------------
    private static final String[] BANKING_KEYWORDS = {
            "credited", "debited", "transaction", "transferred",
            "payment", "spent", "received", "withdrawn",
            "Rs.", "Rs ", "INR", "UPI", "NEFT", "IMPS", "RTGS",
            "a/c", "acct", "account", "bank", "debit", "credit",
            "GPay", "PhonePe", "Paytm", "BHIM", "trf", "sent", "paid"
    };

    public static boolean isBankingSMS(String body) {
        if (body == null || body.isEmpty()) return false;
        String lower = body.toLowerCase(Locale.ENGLISH);
        for (String keyword : BANKING_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase(Locale.ENGLISH))) return true;
        }
        return false;
    }

    // -------------------------------------------------------
    // Amount patterns
    // -------------------------------------------------------
    private static final Pattern[] AMOUNT_PATTERNS = {
            Pattern.compile("(?:Rs\\.?|INR|Rs)\\s*([\\d,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Rs\\.?|INR|Rs)\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+\\.\\d{2})\\s*(?:Rs|INR)", Pattern.CASE_INSENSITIVE),
    };

    // -------------------------------------------------------
    // Type patterns
    // -------------------------------------------------------
    private static final Pattern CREDITED_PATTERN = Pattern.compile(
            "\\b(credited|received|deposited|added|credit)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DEBITED_PATTERN = Pattern.compile(
            "\\b(debited|spent|paid|deducted|withdrawn|transferred|sent|debit)\\b",
            Pattern.CASE_INSENSITIVE);

    // -------------------------------------------------------
    // Date-time patterns — covers most Indian bank formats
    // -------------------------------------------------------
    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})"),
            Pattern.compile("(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2})"),
            Pattern.compile("(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})"),
            Pattern.compile("(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2})"),
            Pattern.compile("(\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})"),
            Pattern.compile("(\\d{2}-(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{2}-(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{2}/\\d{2}/\\d{4})"),
            Pattern.compile("(\\d{2}-\\d{2}-\\d{4})"),
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})"),
    };

    // -------------------------------------------------------
    // Party patterns
    // -------------------------------------------------------
    private static final Pattern VPA_PATTERN = Pattern.compile(
            "([\\w.\\-]+@[a-zA-Z]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern[] PARTY_PATTERNS = {
            Pattern.compile("trf to\\s+([^\\s(,]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("debited from VPA\\s+([^\\s(,]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:to|from|at|by|towards)\\s+([A-Za-z0-9 _\\-\\.]{2,25})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:merchant|shop|store)\\s*[:-]?\\s*([A-Za-z0-9 ]{2,25})", Pattern.CASE_INSENSITIVE),
    };

    // -------------------------------------------------------
    // Reference patterns
    // -------------------------------------------------------
    private static final Pattern[] REFERENCE_PATTERNS = {
            Pattern.compile("RefNo\\s+(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UPI Ref no\\s+(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Ref(?:\\s*No\\.?)?|UTR|Txn(?:\\s*ID)?|Transaction\\s*ID)\\s*[:#]?\\s*([A-Za-z0-9]{6,25})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Ref\\.?\\s*#?)\\s*[:#]?\\s*([A-Za-z0-9]{6,25})", Pattern.CASE_INSENSITIVE),
    };

    // -------------------------------------------------------
    // Main parse method
    // -------------------------------------------------------

    /**
     * Parses a banking SMS into a Transaction object.
     * Returns null if amount or type cannot be extracted.
     *
     * @param body    Raw SMS body text
     * @param smsId   SMS _id from ContentProvider (dedup key)
     * @param smsDate Epoch ms from SMS metadata (used for sorting)
     */
    public static Transaction parse(String body, String smsId, long smsDate) {
        if (body == null || body.isEmpty()) return null;

        // Extract amount — skip if not found
        double amount = extractAmount(body);
        if (amount <= 0) {
            Log.d(TAG, "No amount found — skipping SMS id=" + smsId);
            return null;
        }

        // Extract type — skip if unknown
        String type = extractType(body);
        if ("Unknown".equals(type)) {
            Log.d(TAG, "No type found — skipping SMS id=" + smsId);
            return null;
        }

        // Extract datetime — fallback to SMS metadata timestamp
        String datetime = extractDatetime(body);
        if (datetime == null || datetime.isEmpty()) {
            datetime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date(smsDate));
        }

        // Extract party and reference
        String party     = extractParty(body);
        String reference = extractReference(body);

        // Build default description
        String description = type + " Rs." + String.format(Locale.getDefault(), "%.2f", amount)
                + (party.equals("Unknown") ? ""
                : " " + (type.equals("Credited") ? "from " : "to ") + party);

        Transaction t = new Transaction(datetime, amount, type, description,
                party, reference, smsId, smsDate);
        Log.d(TAG, "Parsed: " + t);
        return t;
    }

    // -------------------------------------------------------
    // Extraction helpers
    // -------------------------------------------------------

    public static double extractAmount(String body) {
        for (Pattern p : AMOUNT_PATTERNS) {
            Matcher m = p.matcher(body);
            if (m.find()) {
                try {
                    return Double.parseDouble(m.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    public static String extractType(String body) {
        if (CREDITED_PATTERN.matcher(body).find()) return "Credited";
        if (DEBITED_PATTERN.matcher(body).find())  return "Debited";
        return "Unknown";
    }

    public static String extractDatetime(String body) {
        for (Pattern p : DATE_PATTERNS) {
            Matcher m = p.matcher(body);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    public static String extractParty(String body) {
        // VPA first (most specific)
        Matcher vpa = VPA_PATTERN.matcher(body);
        if (vpa.find()) return vpa.group(1).trim();
        // Then named patterns
        for (Pattern p : PARTY_PATTERNS) {
            Matcher m = p.matcher(body);
            if (m.find()) return m.group(1).trim();
        }
        return "Unknown";
    }

    public static String extractReference(String body) {
        for (Pattern p : REFERENCE_PATTERNS) {
            Matcher m = p.matcher(body);
            if (m.find()) return m.group(1).trim();
        }
        return "";
    }
}