package net.smartlogic.unitconverter.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Shared ordering keeps a pending delete ahead of Undo across recreation.
    private static final java.util.concurrent.ExecutorService HISTORY_IO = java.util.concurrent.Executors.newSingleThreadExecutor();
    public static void executeHistoryWork(Runnable task) { HISTORY_IO.execute(task); }

    private static final String DATABASE_NAME = "UnitCalc.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_HISTORY = "calculation_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EXPRESSION = "expression";
    private static final String COLUMN_RESULT = "result";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_CALCULATOR_ID = "calculator_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EXPRESSION + " TEXT,"
                + COLUMN_RESULT + " TEXT,"
                + COLUMN_CREATED_AT + " INTEGER,"
                + COLUMN_CALCULATOR_ID + " TEXT"
                + ")";
        db.execSQL(createHistoryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_HISTORY + " ADD COLUMN " + COLUMN_CREATED_AT + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_HISTORY + " ADD COLUMN " + COLUMN_CALCULATOR_ID
                    + " TEXT DEFAULT '" + CalculatorCatalog.ID_BASIC + "'");
        }
    }

    public void addHistory(@NonNull String expression, @NonNull String result, @NonNull String calculatorId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPRESSION, expression);
        values.put(COLUMN_RESULT, result);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        values.put(COLUMN_CALCULATOR_ID, calculatorId);
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    public List<CalculationHistoryItem> getAllHistory() {
        return getHistoryPage(Long.MAX_VALUE, Integer.MAX_VALUE);
    }

    public List<CalculationHistoryItem> getHistoryPage(long beforeId, int limit) {
        List<CalculationHistoryItem> historyList = new ArrayList<>();
        if (limit <= 0) return historyList;
        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.query(TABLE_HISTORY, null, COLUMN_ID + " < ?",
                new String[]{Long.toString(beforeId)}, null, null, COLUMN_ID + " DESC", Integer.toString(limit))) {

        if (cursor.moveToFirst()) {
            do {
                int expressionIndex = cursor.getColumnIndex(COLUMN_EXPRESSION);
                int resultIndex = cursor.getColumnIndex(COLUMN_RESULT);
                int createdIndex = cursor.getColumnIndex(COLUMN_CREATED_AT);
                int calculatorIndex = cursor.getColumnIndex(COLUMN_CALCULATOR_ID);
                if (expressionIndex != -1 && resultIndex != -1) {
                    long createdAt = createdIndex != -1 ? cursor.getLong(createdIndex) : 0L;
                    String calculatorId = calculatorIndex != -1
                            ? cursor.getString(calculatorIndex)
                            : CalculatorCatalog.ID_BASIC;
                    if (calculatorId == null || calculatorId.isEmpty()) {
                        calculatorId = CalculatorCatalog.ID_BASIC;
                    }
                    CalculationHistoryItem item = new CalculationHistoryItem(
                            cursor.getString(expressionIndex),
                            cursor.getString(resultIndex),
                            createdAt,
                            calculatorId,
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    );
                    historyList.add(item);
                }
            } while (cursor.moveToNext());
        }
        } finally { db.close(); }
        return historyList;
    }

    public boolean deleteHistoryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        try { return db.delete(TABLE_HISTORY, COLUMN_ID + " = ?", new String[]{Long.toString(id)}) == 1; }
        finally { db.close(); }
    }

    /** Undo restores the original identity and order, without replacing newer records. */
    public boolean restoreHistoryItem(@NonNull CalculationHistoryItem item) {
        if (item.id() < 0) return false;
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, item.id());
        values.put(COLUMN_EXPRESSION, item.expression());
        values.put(COLUMN_RESULT, item.result());
        values.put(COLUMN_CREATED_AT, item.createdAt());
        values.put(COLUMN_CALCULATOR_ID, item.calculatorId());
        SQLiteDatabase db = getWritableDatabase();
        try { return db.insertWithOnConflict(TABLE_HISTORY, null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1; }
        finally { db.close(); }
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }
}
