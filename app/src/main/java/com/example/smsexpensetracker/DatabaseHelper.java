package com.example.smsexpensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME    = "sms_expenses.db";
    private static final int    DATABASE_VERSION = 2;

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_ID          = "id";
    public static final String COL_DATETIME    = "datetime";
    public static final String COL_AMOUNT      = "amount";
    public static final String COL_TYPE        = "type";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_PARTY       = "party";
    public static final String COL_REFERENCE   = "reference";
    public static final String COL_SMS_ID      = "sms_id";
    public static final String COL_SMS_DATE    = "sms_date";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " ("
                    + COL_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_DATETIME    + " TEXT, "
                    + COL_AMOUNT      + " REAL, "
                    + COL_TYPE        + " TEXT, "
                    + COL_DESCRIPTION + " TEXT, "
                    + COL_PARTY       + " TEXT, "
                    + COL_REFERENCE   + " TEXT, "
                    + COL_SMS_ID      + " TEXT UNIQUE, "
                    + COL_SMS_DATE    + " INTEGER DEFAULT 0"
                    + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "Database created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS
                    + " ADD COLUMN " + COL_SMS_DATE + " INTEGER DEFAULT 0");
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
            onCreate(db);
        }
    }

    public int insertTransactions(List<Transaction> transactions) {
        SQLiteDatabase db = this.getWritableDatabase();
        int count = 0;
        db.beginTransaction();
        try {
            for (Transaction t : transactions) {
                ContentValues cv = buildContentValues(t);
                long rowId = db.insertWithOnConflict(TABLE_TRANSACTIONS, null, cv,
                        SQLiteDatabase.CONFLICT_IGNORE);
                if (rowId != -1) count++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        Log.d(TAG, "Bulk insert: " + count + " new rows.");
        return count;
    }

    /** Inserts a single manually-added transaction. Returns row id or -1 on failure. */
    public long insertSingleTransaction(Transaction t) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = buildContentValues(t);
        long rowId = db.insertWithOnConflict(TABLE_TRANSACTIONS, null, cv,
                SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return rowId;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_TRANSACTIONS,
                null, null, null, null, null,
                COL_SMS_DATE + " DESC, " + COL_ID + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToTransaction(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public int updateDescription(int transactionId, String newDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DESCRIPTION, newDescription);
        int rows = db.update(TABLE_TRANSACTIONS, cv,
                COL_ID + " = ?", new String[]{String.valueOf(transactionId)});
        db.close();
        return rows;
    }

    private ContentValues buildContentValues(Transaction t) {
        ContentValues cv = new ContentValues();
        cv.put(COL_DATETIME,    t.getDatetime());
        cv.put(COL_AMOUNT,      t.getAmount());
        cv.put(COL_TYPE,        t.getType());
        cv.put(COL_DESCRIPTION, t.getDescription());
        cv.put(COL_PARTY,       t.getParty());
        cv.put(COL_REFERENCE,   t.getReference());
        cv.put(COL_SMS_ID,      t.getSmsId());
        cv.put(COL_SMS_DATE,    t.getSmsDate());
        return cv;
    }

    private Transaction cursorToTransaction(Cursor cursor) {
        Transaction t = new Transaction();
        t.setId(         cursor.getInt(   cursor.getColumnIndexOrThrow(COL_ID)));
        t.setDatetime(   cursor.getString(cursor.getColumnIndexOrThrow(COL_DATETIME)));
        t.setAmount(     cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        t.setType(       cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)));
        t.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
        t.setParty(      cursor.getString(cursor.getColumnIndexOrThrow(COL_PARTY)));
        t.setReference(  cursor.getString(cursor.getColumnIndexOrThrow(COL_REFERENCE)));
        t.setSmsId(      cursor.getString(cursor.getColumnIndexOrThrow(COL_SMS_ID)));
        t.setSmsDate(    cursor.getLong(  cursor.getColumnIndexOrThrow(COL_SMS_DATE)));
        return t;
    }
}