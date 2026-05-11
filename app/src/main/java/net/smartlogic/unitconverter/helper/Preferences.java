package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.content.SharedPreferences;

import net.smartlogic.unitconverter.R;

import androidx.preference.PreferenceManager;

public class Preferences {

    public static final String PREFS_THEME = "pref_theme";
    public static final String PREFS_NUMBER_OF_DECIMALS = "number_decimals";
    public static final String PREFS_DECIMAL_SEPARATOR = "decimal_separator";
    public static final String PREFS_GROUP_SEPARATOR = "group_separator";
    private static final String PREFS_LAST_CONVERSION = "last_conversion";
    private static final String PREFS_LAST_FROM_UNIT = "last_from_unit";
    private static final String PREFS_LAST_TO_UNIT = "last_to_unit";

    public static final String PREFS_CURR_LAST_UPDT = "currency_last_update";
    public static final String PREFS_CURR_DATA = "currency_data";
    public static final String PREFS_CURR_FROM_INDEX = "from_currency_index";
    public static final String PREFS_CURR_TO_INDEX = "to_currency_index";

    private static Preferences mInstance;
    private final SharedPreferences mPrefs;
    private final Context mContext;

    public static Preferences getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Preferences(context.getApplicationContext());
        }

        return mInstance;
    }

    private Preferences(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
    }

    public SharedPreferences getPreferences() {
        return mPrefs;
    }

    public boolean isLightTheme() {
        return mPrefs.getBoolean(PREFS_THEME, false);
    }

    public String getPrefsTheme() {
        try {
            if (mPrefs.getBoolean(PREFS_THEME, false)) {
                return ThemeHelper.DARK_MODE;
            }
        }
        catch (Exception ignored) {
            return ThemeHelper.LIGHT_MODE;
        }
        return ThemeHelper.LIGHT_MODE;
    }

    public int getLastConversion() {
        return mPrefs.getInt(PREFS_LAST_CONVERSION, 0);
    }

    public void setLastConversion(int conversionId) {
        mPrefs.edit().putInt(PREFS_LAST_CONVERSION, conversionId).apply();
    }

    public int getLastFromConversion() {
        return mPrefs.getInt(PREFS_LAST_FROM_UNIT, 0);
    }

    public void setLastFromConversion(int conversionId) {
        mPrefs.edit().putInt(PREFS_LAST_FROM_UNIT, conversionId).apply();
    }

    public int getLastToConversion() {
        return mPrefs.getInt(PREFS_LAST_TO_UNIT, 1);
    }

    public void setLastToConversion(int conversionId) {
        mPrefs.edit().putInt(PREFS_LAST_TO_UNIT, conversionId).apply();
    }

    public int getNumberDecimals() {
        return Integer.parseInt(mPrefs.getString(PREFS_NUMBER_OF_DECIMALS, mContext.getString(R.string.default_number_decimals)));
    }

    public String getDecimalSeparator() {
        return mPrefs.getString(PREFS_DECIMAL_SEPARATOR, mContext.getString(R.string.default_decimal_separator));
    }

    public String getGroupSeparator() {
        return mPrefs.getString(PREFS_GROUP_SEPARATOR, mContext.getString(R.string.default_group_separator));
    }

    public void setCurrencyLastUpdateDate(long  timeInMilli) {
        mPrefs.edit().putLong(PREFS_CURR_LAST_UPDT, timeInMilli).apply();
    }

    public long getCurrencyLastUpdateDate() {
        return mPrefs.getLong(PREFS_CURR_LAST_UPDT,0);
    }

    public void setCurrencyResponse(String response) {
        mPrefs.edit().putString(PREFS_CURR_DATA, response).apply();
    }

    public String getCurrencyResponse() {
        return  mPrefs.getString(PREFS_CURR_DATA,"");
    }

    public void setFromCurrencyIndex(int index) {
        mPrefs.edit().putInt(PREFS_CURR_FROM_INDEX, index).apply();
    }

    public int getFromCurrencyIndex() {
        return mPrefs.getInt(PREFS_CURR_FROM_INDEX, 31);
    }

    public void setToCurrencyIndex(int index) {
        mPrefs.edit().putInt(PREFS_CURR_TO_INDEX, index).apply();
    }

    public int getToCurrencyIndex() {
        return mPrefs.getInt(PREFS_CURR_TO_INDEX, 32);
    }
}