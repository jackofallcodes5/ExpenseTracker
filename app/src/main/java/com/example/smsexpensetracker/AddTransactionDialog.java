package com.example.smsexpensetracker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

/**
 * AddTransactionDialog.java
 * Dialog for manually adding a cash transaction.
 * Fields: Date, Time, Type (Credit/Debit), Amount, Party, Description
 */
public class AddTransactionDialog extends Dialog {

    public interface OnTransactionAddedListener {
        void onAdded();
    }

    private final DatabaseHelper db;
    private final OnTransactionAddedListener listener;

    private EditText  etDate, etTime, etAmount, etParty, etDescription;
    private RadioGroup rgType;
    private RadioButton rbCredit, rbDebit;
    private Button btnPickDate, btnPickTime, btnSave, btnCancel;

    private int selectedYear, selectedMonth, selectedDay;
    private int selectedHour, selectedMinute;

    public AddTransactionDialog(@NonNull Context context,
                                @NonNull DatabaseHelper db,
                                OnTransactionAddedListener listener) {
        super(context);
        this.db       = db;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getWindow() != null)
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.dialog_add_transaction);

        bindViews();
        prefillCurrentDateTime();
        setClickListeners();
    }

    private void bindViews() {
        etDate        = findViewById(R.id.etDate);
        etTime        = findViewById(R.id.etTime);
        etAmount      = findViewById(R.id.etAmount);
        etParty       = findViewById(R.id.etParty);
        etDescription = findViewById(R.id.etAddDescription);
        rgType        = findViewById(R.id.rgType);
        rbCredit      = findViewById(R.id.rbCredit);
        rbDebit       = findViewById(R.id.rbDebit);
        btnPickDate   = findViewById(R.id.btnPickDate);
        btnPickTime   = findViewById(R.id.btnPickTime);
        btnSave       = findViewById(R.id.btnAddSave);
        btnCancel     = findViewById(R.id.btnAddCancel);
    }

    /** Pre-fills date and time fields with the current date/time */
    private void prefillCurrentDateTime() {
        Calendar cal = Calendar.getInstance();
        selectedYear   = cal.get(Calendar.YEAR);
        selectedMonth  = cal.get(Calendar.MONTH);
        selectedDay    = cal.get(Calendar.DAY_OF_MONTH);
        selectedHour   = cal.get(Calendar.HOUR_OF_DAY);
        selectedMinute = cal.get(Calendar.MINUTE);

        updateDateField();
        updateTimeField();

        // Default to Debit selected
        rbDebit.setChecked(true);
    }

    private void setClickListeners() {

        // Date picker
        btnPickDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    getContext(),
                    (view, year, month, day) -> {
                        selectedYear  = year;
                        selectedMonth = month;
                        selectedDay   = day;
                        updateDateField();
                    },
                    selectedYear, selectedMonth, selectedDay
            );
            picker.show();
        });

        // Time picker
        btnPickTime.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(
                    getContext(),
                    (view, hour, minute) -> {
                        selectedHour   = hour;
                        selectedMinute = minute;
                        updateTimeField();
                    },
                    selectedHour, selectedMinute, true  // 24-hour format
            );
            picker.show();
        });

        btnSave.setOnClickListener(v -> saveTransaction());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void updateDateField() {
        etDate.setText(String.format(Locale.getDefault(),
                "%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear));
    }

    private void updateTimeField() {
        etTime.setText(String.format(Locale.getDefault(),
                "%02d:%02d:00", selectedHour, selectedMinute));
    }

    private void saveTransaction() {
        // --- Validate Amount ---
        String amountStr = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "Please enter an amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Enter a valid amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Validate Date / Time ---
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(getContext(), "Please select date and time.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Type ---
        String type = rbCredit.isChecked() ? "Credited" : "Debited";

        // --- Optional fields ---
        String party       = etParty.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (party.isEmpty())       party       = "Cash";
        if (description.isEmpty()) description = type + " Rs." + String.format(Locale.getDefault(), "%.2f", amount)
                + " (" + party + ")";

        String datetime = date + " " + time;

        // Use current timestamp as smsDate for sorting (cash entries appear at top)
        long smsDate = System.currentTimeMillis();

        // Unique ID for cash entries: "CASH_" + timestamp
        String smsId = "CASH_" + smsDate;

        Transaction t = new Transaction(datetime, amount, type,
                description, party, "", smsId, smsDate);

        long result = db.insertSingleTransaction(t);
        if (result != -1) {
            Toast.makeText(getContext(), "Transaction added!", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onAdded();
            dismiss();
        } else {
            Toast.makeText(getContext(), "Failed to save. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}