package com.example.smsexpensetracker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SMSReader {

    private static final String TAG = "SMSReader";
    private static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");
    private static final String[] SMS_COLUMNS = {"_id", "address", "body", "date"};

    public static List<Transaction> readBankingTransactions(Context context) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(
                    SMS_INBOX_URI,
                    SMS_COLUMNS,
                    null,
                    null,
                    "date ASC"   // ← oldest SMS inserted first → gets lowest ID
                    //   newest SMS inserted last  → gets highest ID
                    //   so sorting by id DESC = newest on top
            );

            if (cursor == null) {
                Log.w(TAG, "Null cursor – READ_SMS permission may be missing.");
                return transactions;
            }

            Log.d(TAG, "Total inbox SMS: " + cursor.getCount());

            int colId   = cursor.getColumnIndex("_id");
            int colBody = cursor.getColumnIndex("body");
            int colDate = cursor.getColumnIndex("date");

            int parsed = 0, skipped = 0;

            while (cursor.moveToNext()) {
                String smsId = cursor.getString(colId);
                String body  = cursor.getString(colBody);
                long   date  = cursor.getLong(colDate);

                if (body == null || body.trim().isEmpty()) { skipped++; continue; }
                if (!SMSParser.isBankingSMS(body))         { skipped++; continue; }

                Transaction t = SMSParser.parse(body, smsId, date);
                if (t != null) { transactions.add(t); parsed++; }
                else           { skipped++; }
            }

            Log.i(TAG, "Scan complete – Parsed: " + parsed + " | Skipped: " + skipped);

        } catch (SecurityException e) {
            Log.e(TAG, "READ_SMS permission denied.", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error reading SMS.", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }

        return transactions;
    }
}