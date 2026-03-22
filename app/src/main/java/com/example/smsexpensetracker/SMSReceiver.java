package com.example.smsexpensetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {

    private static final String TAG          = "SMSReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SMS_RECEIVED.equals(intent.getAction())) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        try {
            Object[] pdus  = (Object[]) bundle.get("pdus");
            String   format = bundle.getString("format");
            if (pdus == null || pdus.length == 0) return;

            StringBuilder fullBody = new StringBuilder();
            String sender    = "";
            long   timestamp = System.currentTimeMillis();

            for (Object pdu : pdus) {
                SmsMessage sms;

                // ✅ Fix: use two-param method on API 23+, fallback on API 21-22
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                } else {
                    sms = SmsMessage.createFromPdu((byte[]) pdu);
                }

                if (sms != null) {
                    fullBody.append(sms.getMessageBody());
                    sender    = sms.getOriginatingAddress();
                    timestamp = sms.getTimestampMillis();
                }
            }

            String body  = fullBody.toString();
            String smsId = "LIVE_" + timestamp + "_"
                    + (sender != null ? sender.hashCode() : 0);

            Log.d(TAG, "Incoming SMS from: " + sender);

            if (!SMSParser.isBankingSMS(body)) {
                Log.d(TAG, "Not a banking SMS — ignored.");
                return;
            }

            Transaction t = SMSParser.parse(body, smsId, timestamp);
            if (t != null) {
                DatabaseHelper db = new DatabaseHelper(context);
                long result = db.insertSingleTransaction(t);
                if (result != -1) {
                    Log.i(TAG, "New transaction saved: " + t);
                } else {
                    Log.d(TAG, "Duplicate — skipped.");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "SMSReceiver error: " + e.getMessage(), e);
        }
    }
}