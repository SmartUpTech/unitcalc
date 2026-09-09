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
        List<CalculationHistoryItem> historyList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

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
                            calculatorId
                    );
                    historyList.add(item);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }
}
