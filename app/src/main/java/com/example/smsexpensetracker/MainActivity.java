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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG               = "MainActivity";
    private static final int    REQUEST_PERMISSIONS = 101;
    private static final String PREFS_NAME          = "sms_tracker_prefs";
    private static final String PREF_FIRST_LAUNCH   = "first_launch_done";

    private RecyclerView         recyclerView;
    private TransactionAdapter   adapter;
    private LinearLayout         layoutProgress;
    private LinearLayout         layoutEmpty;
    private TextView             tvSummary;
    private FloatingActionButton fabRescan;
    private FloatingActionButton fabAddTransaction;
    private AdView               adView;

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
        initAdMob();

        // Request READ_SMS + RECEIVE_SMS together
        if (hasAllPermissions()) {
            handleFirstLaunch();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS
                    },
                    REQUEST_PERMISSIONS
            );
        }

        // Reload FAB — manually re-scans inbox for missed SMS
        if (fabRescan != null) {
            fabRescan.setOnClickListener(v -> {
                if (hasAllPermissions()) {
                    new ImportSMSTask().execute();
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                            },
                            REQUEST_PERMISSIONS
                    );
                }
            });
        }

        // Add Transaction FAB — opens manual cash entry dialog
        if (fabAddTransaction != null) {
            fabAddTransaction.setOnClickListener(v -> {
                AddTransactionDialog dialog = new AddTransactionDialog(
                        this,
                        dbHelper,
                        () -> {
                            loadFromDatabase();
                            Toast.makeText(this, "Transaction added!",
                                    Toast.LENGTH_SHORT).show();
                        }
                );
                dialog.show();
            });
        }
    }

    // -------------------------------------------------------
    // onResume — refresh list every time app comes to foreground
    // This picks up any transactions saved by SMSReceiver
    // while the app was in the background or closed
    // -------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        // Always reload from DB on resume —
        // SMSReceiver may have saved new transactions while app was closed
        if (dbHelper != null && adapter != null) {
            loadFromDatabase();
        }
        if (adView != null) adView.resume();
    }

    // -------------------------------------------------------
    // AdMob lifecycle
    // -------------------------------------------------------

    private void initAdMob() {
        MobileAds.initialize(this, initializationStatus ->
                Log.d(TAG, "AdMob initialized."));
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    // -------------------------------------------------------
    // Permission result
    // -------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean readSmsGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_SMS.equals(permissions[i])
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    readSmsGranted = true;
                }
            }
            if (readSmsGranted) {
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
    // First launch — full SMS scan only once
    // After that, SMSReceiver handles everything in real time
    // -------------------------------------------------------

    private void handleFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean done = prefs.getBoolean(PREF_FIRST_LAUNCH, false);
        if (!done) {
            // First install — scan full inbox to import history
            prefs.edit().putBoolean(PREF_FIRST_LAUNCH, true).apply();
            new ImportSMSTask().execute();
        } else {
            // All subsequent launches — just load from DB instantly
            // New SMS are already saved by SMSReceiver in background
            loadFromDatabase();
        }
    }

    // -------------------------------------------------------
    // RecyclerView setup
    // -------------------------------------------------------

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        adapter = new TransactionAdapter(this, transactionList, transaction -> {
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
        adView            = findViewById(R.id.adView);
    }

    // -------------------------------------------------------
    // Permission helpers
    // -------------------------------------------------------

    /** Returns true only if BOTH READ_SMS and RECEIVE_SMS are granted */
    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // -------------------------------------------------------
    // AsyncTask — background full SMS import (first launch only)
    // -------------------------------------------------------

    private class ImportSMSTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            if (layoutProgress != null)    layoutProgress.setVisibility(View.VISIBLE);
            if (fabRescan != null)         fabRescan.setEnabled(false);
            if (fabAddTransaction != null) fabAddTransaction.setEnabled(false);
            if (layoutEmpty != null)       layoutEmpty.setVisibility(View.GONE);
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