package com.example.smsexpensetracker;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG               = "MainActivity";
    private static final int    REQUEST_READ_SMS  = 101;
    private static final String PREFS_NAME        = "sms_tracker_prefs";
    private static final String PREF_FIRST_LAUNCH = "first_launch_done";

    private RecyclerView         recyclerView;
    private TransactionAdapter   adapter;
    private LinearLayout         layoutProgress;
    private LinearLayout         layoutEmpty;
    private TextView             tvSummary;
    private FloatingActionButton fabRescan;
    private FloatingActionButton fabAddTransaction;

    private DatabaseHelper    dbHelper;
    private List<Transaction> transactionList = new ArrayList<>();

    // -------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            Log.e(TAG, "setContentView failed: " + e.getMessage(), e);
            return;
        }

        // Setup toolbar
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) setSupportActionBar(toolbar);
        } catch (Exception e) {
            Log.e(TAG, "Toolbar setup failed: " + e.getMessage(), e);
        }

        dbHelper = new DatabaseHelper(this);

        bindViews();
        setupRecyclerView();

        // Request SMS permission or handle first launch
        if (hasSMSPermission()) {
            handleFirstLaunch();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_SMS},
                    REQUEST_READ_SMS
            );
        }

        // Reload FAB — re-scans SMS inbox
        if (fabRescan != null) {
            fabRescan.setOnClickListener(v -> {
                if (hasSMSPermission()) {
                    new ImportSMSTask().execute();
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.READ_SMS},
                            REQUEST_READ_SMS
                    );
                }
            });
        }

        // Add Transaction FAB — opens manual entry dialog
        if (fabAddTransaction != null) {
            fabAddTransaction.setOnClickListener(v -> {
                AddTransactionDialog dialog = new AddTransactionDialog(
                        this,
                        dbHelper,
                        () -> {
                            loadFromDatabase();
                            Toast.makeText(this, "Transaction added!", Toast.LENGTH_SHORT).show();
                        }
                );
                dialog.show();
            });
        }
    }

    // -------------------------------------------------------
    // Permission result
    // -------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_SMS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Scanning...",
                        Toast.LENGTH_SHORT).show();
                handleFirstLaunch();
            } else {
                Toast.makeText(this, "SMS permission denied.",
                        Toast.LENGTH_LONG).show();
                loadFromDatabase();
            }
        }
    }

    // -------------------------------------------------------
    // First launch
    // -------------------------------------------------------

    /**
     * On first install: automatically scan all SMS.
     * On subsequent launches: just load from local database.
     */
    private void handleFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean done = prefs.getBoolean(PREF_FIRST_LAUNCH, false);
        if (!done) {
            prefs.edit().putBoolean(PREF_FIRST_LAUNCH, true).apply();
            new ImportSMSTask().execute();
        } else {
            loadFromDatabase();
        }
    }

    // -------------------------------------------------------
    // RecyclerView setup
    // -------------------------------------------------------

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        adapter = new TransactionAdapter(this, transactionList, transaction -> {
            // Row click → open detail dialog
            try {
                TransactionDialog dialog = new TransactionDialog(
                        this,
                        transaction,
                        dbHelper,
                        () -> {
                            loadFromDatabase();
                            Toast.makeText(this, "Description updated.",
                                    Toast.LENGTH_SHORT).show();
                        }
                );
                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Dialog open failed: " + e.getMessage(), e);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    // -------------------------------------------------------
    // Load data from SQLite into RecyclerView
    // -------------------------------------------------------

    private void loadFromDatabase() {
        try {
            List<Transaction> list = dbHelper.getAllTransactions();
            transactionList.clear();
            transactionList.addAll(list);
            if (adapter != null) adapter.updateData(transactionList);
            updateSummaryBar();
            updateEmptyState();
        } catch (Exception e) {
            Log.e(TAG, "loadFromDatabase failed: " + e.getMessage(), e);
        }
    }

    /** Shows total count, total credited, total debited in the summary bar */
    private void updateSummaryBar() {
        if (tvSummary == null) return;
        double credit = 0, debit = 0;
        for (Transaction t : transactionList) {
            if ("Credited".equalsIgnoreCase(t.getType()))     credit += t.getAmount();
            else if ("Debited".equalsIgnoreCase(t.getType())) debit  += t.getAmount();
        }
        tvSummary.setText(String.format(
                "%d Transactions  |  + Rs.%.0f  |  - Rs.%.0f",
                transactionList.size(), credit, debit));
    }

    private void updateEmptyState() {
        if (layoutEmpty == null || recyclerView == null) return;
        if (transactionList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // -------------------------------------------------------
    // View binding
    // -------------------------------------------------------

    private void bindViews() {
        recyclerView      = findViewById(R.id.recyclerView);
        layoutProgress    = findViewById(R.id.progressBar);
        layoutEmpty       = findViewById(R.id.tvEmptyState);
        tvSummary         = findViewById(R.id.tvSummary);
        fabRescan         = findViewById(R.id.fabRescan);
        fabAddTransaction = findViewById(R.id.fabAddTransaction);
    }

    private boolean hasSMSPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // -------------------------------------------------------
    // AsyncTask — background SMS import
    // -------------------------------------------------------

    private class ImportSMSTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            if (layoutProgress != null) layoutProgress.setVisibility(View.VISIBLE);
            if (fabRescan != null)      fabRescan.setEnabled(false);
            if (fabAddTransaction != null) fabAddTransaction.setEnabled(false);
            if (layoutEmpty != null)    layoutEmpty.setVisibility(View.GONE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                List<Transaction> parsed =
                        SMSReader.readBankingTransactions(MainActivity.this);
                return dbHelper.insertTransactions(parsed);
            } catch (Exception e) {
                Log.e(TAG, "ImportSMSTask error: " + e.getMessage(), e);
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer newRows) {
            if (layoutProgress != null)    layoutProgress.setVisibility(View.GONE);
            if (fabRescan != null)         fabRescan.setEnabled(true);
            if (fabAddTransaction != null) fabAddTransaction.setEnabled(true);
            loadFromDatabase();
            String msg = newRows > 0
                    ? newRows + " new transaction(s) imported!"
                    : "No new transactions found.";
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        }
    }
}