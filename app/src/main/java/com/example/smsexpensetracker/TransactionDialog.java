package com.example.smsexpensetracker;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * TransactionDialog.java
 * A custom Dialog that displays full transaction details and allows
 * the user to edit and save the description field.
 *
 * Usage:
 *   new TransactionDialog(context, transaction, db, () -> refreshList()).show();
 */
public class TransactionDialog extends Dialog {

    // -------------------------------------------------------
    // Callback interface
    // -------------------------------------------------------

    /** Called after a description is successfully saved, so the list can refresh. */
    public interface OnDescriptionSavedListener {
        void onSaved();
    }

    // -------------------------------------------------------
    // Fields
    // -------------------------------------------------------
    private final Transaction transaction;
    private final DatabaseHelper db;
    private final OnDescriptionSavedListener listener;

    // Views
    private TextView  tvDialogDatetime;
    private TextView  tvDialogAmount;
    private TextView  tvDialogType;
    private TextView  tvDialogParty;
    private TextView  tvDialogReference;
    private EditText  etDialogDescription;
    private Button    btnSave;
    private Button    btnClose;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    /**
     * @param context     Activity context
     * @param transaction The transaction to display
     * @param db          DatabaseHelper instance for updating description
     * @param listener    Callback after save (can be null)
     */
    public TransactionDialog(@NonNull Context context,
                             @NonNull Transaction transaction,
                             @NonNull DatabaseHelper db,
                             OnDescriptionSavedListener listener) {
        super(context);
        this.transaction = transaction;
        this.db          = db;
        this.listener    = listener;
    }

    // -------------------------------------------------------
    // Dialog lifecycle
    // -------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove default title bar and set transparent background for rounded corners
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setContentView(R.layout.dialog_transaction);

        bindViews();
        populateData();
        setClickListeners();
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    /** Binds all view references. */
    private void bindViews() {
        tvDialogDatetime    = findViewById(R.id.tvDialogDatetime);
        tvDialogAmount      = findViewById(R.id.tvDialogAmount);
        tvDialogType        = findViewById(R.id.tvDialogType);
        tvDialogParty       = findViewById(R.id.tvDialogParty);
        tvDialogReference   = findViewById(R.id.tvDialogReference);
        etDialogDescription = findViewById(R.id.etDialogDescription);
        btnSave             = findViewById(R.id.btnSave);
        btnClose            = findViewById(R.id.btnClose);
    }

    /** Populates views with data from the Transaction object. */
    private void populateData() {
        tvDialogDatetime.setText(transaction.getDatetime());

        // Format amount with INR symbol and 2 decimal places
        tvDialogAmount.setText(String.format("₹ %.2f", transaction.getAmount()));

        // Color the type label: green for Credited, red for Debited
        String type = transaction.getType();
        tvDialogType.setText(type);
        if ("Credited".equalsIgnoreCase(type)) {
            tvDialogType.setTextColor(Color.parseColor("#2E7D32")); // Dark green
        } else if ("Debited".equalsIgnoreCase(type)) {
            tvDialogType.setTextColor(Color.parseColor("#C62828")); // Dark red
        } else {
            tvDialogType.setTextColor(Color.parseColor("#455A64")); // Grey
        }

        tvDialogParty.setText(
                TextUtils.isEmpty(transaction.getParty()) ? "N/A" : transaction.getParty()
        );
        tvDialogReference.setText(
                TextUtils.isEmpty(transaction.getReference()) ? "N/A" : transaction.getReference()
        );
        etDialogDescription.setText(transaction.getDescription());
        // Move cursor to end of existing text
        etDialogDescription.setSelection(etDialogDescription.getText().length());
    }

    /** Sets click listeners for Save and Close buttons. */
    private void setClickListeners() {
        btnSave.setOnClickListener(v -> saveDescription());
        btnClose.setOnClickListener(v -> dismiss());
    }

    /**
     * Reads the description from EditText, updates SQLite,
     * updates the in-memory Transaction object, and notifies the listener.
     */
    private void saveDescription() {
        String newDesc = etDialogDescription.getText().toString().trim();

        if (TextUtils.isEmpty(newDesc)) {
            Toast.makeText(getContext(), "Description cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        int rows = db.updateDescription(transaction.getId(), newDesc);
        if (rows > 0) {
            transaction.setDescription(newDesc);  // Keep in-memory model in sync
            Toast.makeText(getContext(), "Description saved!", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onSaved();
            dismiss();
        } else {
            Toast.makeText(getContext(), "Failed to save. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
