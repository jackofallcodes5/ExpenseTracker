package com.example.smsexpensetracker;

import android.util.Log;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMSParser.java
 * Strictly parses only DNS Bank transaction SMS in these two formats:
 *
 * DEBIT:
 * "Your a/c no. XX0046 debited for Rs.34.00 on 17-03-2026 19:17:24
 *  trf to bharatpe.9q0p0e0k0c835361@unit (RefNo 120174243512). ... -DNS Bank"
 *
 * CREDIT:
 * "Your a/c no. XX0046 is credited for Rs.200.00 on 14-03-2026 18:27:20
 *  and debited from VPA sandeep68fdal@okaxis (UPI Ref no 643950516646 )-DNS Bank"
 */
public class SMSParser {

    private static final String TAG = "SMSParser";

    // -------------------------------------------------------
    // Strict format validator
    // Must contain ALL of these to be considered a valid bank SMS
    // -------------------------------------------------------
    private static final Pattern VALID_SMS_PATTERN = Pattern.compile(
            "Your a/c no\\.\\s*XX\\d+.*(?:debited for|credited for)\\s*Rs\\.\\d+.*DNS Bank",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * Returns true ONLY if the SMS strictly matches DNS Bank transaction format.
     * Rejects promotional, recharge suggestion, and all other SMS.
     */
    public static boolean isBankingSMS(String body) {
        if (body == null || body.isEmpty()) return false;
        return VALID_SMS_PATTERN.matcher(body).find();
    }

    // -------------------------------------------------------
    // Extraction Patterns — matched to your exact SMS format
    // -------------------------------------------------------

    // Amount: Rs.34.00 or Rs.200.00
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "Rs\\.([\\d,]+\\.\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    // Date-time: 17-03-2026 19:17:24
    private static final Pattern DATETIME_PATTERN = Pattern.compile(
            "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})"
    );

    // Type: "debited for" → Debited, "credited for" → Credited
    private static final Pattern DEBIT_PATTERN = Pattern.compile(
            "debited for", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
            "credited for", Pattern.CASE_INSENSITIVE
    );

    // Party for DEBIT: "trf to bharatpe.9q0p0e0k0c835361@unit"
    private static final Pattern DEBIT_PARTY_PATTERN = Pattern.compile(
            "trf to\\s+([^\\s(]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Party for CREDIT: "debited from VPA sandeep68fdal@okaxis"
    private static final Pattern CREDIT_PARTY_PATTERN = Pattern.compile(
            "debited from VPA\\s+([^\\s(]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Reference for DEBIT: "(RefNo 120174243512)"
    private static final Pattern DEBIT_REF_PATTERN = Pattern.compile(
            "RefNo\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // Reference for CREDIT: "(UPI Ref no 643950516646)"
    private static final Pattern CREDIT_REF_PATTERN = Pattern.compile(
            "UPI Ref no\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // -------------------------------------------------------
    // Main parse method
    // -------------------------------------------------------

    /**
     * Parses a DNS Bank SMS into a Transaction object.
     * Returns null if the SMS does not match the strict format.
     *
     * @param body    Raw SMS body text
     * @param smsId   SMS _id from ContentProvider (dedup key)
     * @param smsDate Epoch ms from SMS metadata (used for sorting)
     */
    public static Transaction parse(String body, String smsId, long smsDate) {
        if (body == null || body.isEmpty()) return null;

        // Reject anything that doesn't match our strict format
        if (!isBankingSMS(body)) {
            Log.d(TAG, "Rejected non-bank SMS id=" + smsId);
            return null;
        }

        // 1. Extract Amount
        double amount = extractAmount(body);
        if (amount <= 0) {
            Log.d(TAG, "No valid amount in SMS id=" + smsId);
            return null;
        }

        // 2. Extract Type
        String type = extractType(body);

        // 3. Extract DateTime from SMS body
        String datetime = extractDatetime(body);
        if (datetime == null || datetime.isEmpty()) {
            // Fallback to SMS metadata timestamp
            datetime = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss",
                    java.util.Locale.getDefault()).format(new java.util.Date(smsDate));
        }

        // 4. Extract Party (VPA) based on type
        String party = extractParty(body, type);

        // 5. Extract Reference Number based on type
        String reference = extractReference(body, type);

        // 6. Build description
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
        Matcher m = AMOUNT_PATTERN.matcher(body);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Amount parse error: " + e.getMessage());
            }
        }
        return 0;
    }

    public static String extractType(String body) {
        if (CREDIT_PATTERN.matcher(body).find()) return "Credited";
        if (DEBIT_PATTERN.matcher(body).find())  return "Debited";
        return "Unknown";
    }

    public static String extractDatetime(String body) {
        Matcher m = DATETIME_PATTERN.matcher(body);
        if (m.find()) return m.group(1);
        return null;
    }

    public static String extractParty(String body, String type) {
        if ("Debited".equals(type)) {
            // trf to bharatpe.9q0p0e0k0c835361@unit
            Matcher m = DEBIT_PARTY_PATTERN.matcher(body);
            if (m.find()) return m.group(1).trim();
        } else if ("Credited".equals(type)) {
            // debited from VPA sandeep68fdal@okaxis
            Matcher m = CREDIT_PARTY_PATTERN.matcher(body);
            if (m.find()) return m.group(1).trim();
        }
        return "Unknown";
    }

    public static String extractReference(String body, String type) {
        if ("Debited".equals(type)) {
            // RefNo 120174243512
            Matcher m = DEBIT_REF_PATTERN.matcher(body);
            if (m.find()) return m.group(1).trim();
        } else if ("Credited".equals(type)) {
            // UPI Ref no 643950516646
            Matcher m = CREDIT_REF_PATTERN.matcher(body);
            if (m.find()) return m.group(1).trim();
        }
        return "";
    }
}