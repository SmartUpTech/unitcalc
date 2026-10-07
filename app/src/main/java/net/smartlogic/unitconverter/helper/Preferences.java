package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeProgression;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

public class Preferences {

    public static final String PREFS_SELECTED_THEME = "selected_theme";
    public static final String PREFS_THEME_USAGE_DAYS = "theme_usage_days";
    private static final String PREFS_THEME_LAST_USAGE_DATE = "theme_last_usage_date";
    private static final String PREFS_THEME_PROGRESSION_VERSION = "theme_progression_version";
    private static final String PREFS_THEME_GRANDFATHERED_ID = "theme_grandfathered_id";
    public static final String PREFS_NUMBER_OF_DECIMALS = "number_decimals";
    public static final String PREFS_DECIMAL_SEPARATOR = "decimal_separator";
    public static final String PREFS_GROUP_SEPARATOR = "group_separator";
    public static final String PREFS_KEY_SOUNDS = "key_sounds";
    public static final String PREFS_KEY_VIBRATION = "key_vibration";
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
    private final ThemeProgression themeProgression = new ThemeProgression(CalculatorThemes.unlockRanks());

    public static synchronized Preferences getInstance(Context context) {
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

    public boolean isKeySoundsEnabled() {
        return mPrefs.getBoolean(PREFS_KEY_SOUNDS, true);
    }

    public boolean isKeyVibrationEnabled() {
        return mPrefs.getBoolean(PREFS_KEY_VIBRATION, true);
    }

    public void setKeySoundsEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(PREFS_KEY_SOUNDS, enabled).apply();
    }

    public void setKeyVibrationEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(PREFS_KEY_VIBRATION, enabled).apply();
    }

    public String getSelectedThemeId() {
        try {
            return CalculatorThemes.fromId(mPrefs.getString(PREFS_SELECTED_THEME, CalculatorThemes.DEFAULT_ID)).id();
        } catch (ClassCastException invalidType) {
            return CalculatorThemes.DEFAULT_ID;
        }
    }

    public synchronized boolean setSelectedThemeId(String themeId) {
        if (!themeProgression.isThemeUnlocked(themeId, getThemeProgression())) return false;
        if (themeId.equals(getSelectedThemeId())) return true;
        mPrefs.edit().putString(PREFS_SELECTED_THEME, themeId).apply();
        return true;
    }

    public synchronized ThemeProgression.State getThemeProgression() {
        return updateThemeProgression(null);
    }

    public synchronized ThemeProgression.State recordThemeUsage(LocalDate localDate) {
        return updateThemeProgression(Objects.requireNonNull(localDate));
    }

    /** One atomic preference batch for migration + usage. SharedPreferences remains the store. */
    private ThemeProgression.State updateThemeProgression(LocalDate localDate) {
        Map<String, ?> values = mPrefs.getAll();
        Object rawDays = values.get(PREFS_THEME_USAGE_DAYS);
        Object rawVersion = values.get(PREFS_THEME_PROGRESSION_VERSION);
        Object rawDate = values.get(PREFS_THEME_LAST_USAGE_DATE);
        Object rawGrandfathered = values.get(PREFS_THEME_GRANDFATHERED_ID);
        Object rawSelected = values.get(PREFS_SELECTED_THEME);
        LocalDate lastDate = null;
        if (rawDate instanceof String) {
            try {
                lastDate = LocalDate.parse((String) rawDate);
            } catch (DateTimeParseException invalidDate) {
                // Corrupt date: resume from the next actual launch without losing earned days.
            }
        }
        ThemeProgression.State stored = new ThemeProgression.State(
                rawDays instanceof Integer ? (Integer) rawDays : 0, lastDate,
                rawVersion instanceof Integer ? (Integer) rawVersion : 0,
                rawGrandfathered instanceof String ? (String) rawGrandfathered : null);
        String selectedId = CalculatorThemes.fromId(rawSelected instanceof String ? (String) rawSelected : null).id();
        ThemeProgression.State updated = themeProgression.migrate(stored, selectedId,
                rawSelected instanceof String && rawSelected.equals(selectedId));
        if (localDate != null) updated = themeProgression.registerUsage(updated, localDate);
        boolean repairSelection = values.containsKey(PREFS_SELECTED_THEME) && !selectedId.equals(rawSelected);
        if (!updated.equals(stored) || repairSelection) {
            SharedPreferences.Editor editor = mPrefs.edit()
                    .putInt(PREFS_THEME_USAGE_DAYS, updated.totalUsageDays())
                    .putInt(PREFS_THEME_PROGRESSION_VERSION, updated.version());
            if (updated.lastCountedDate() != null) {
                editor.putString(PREFS_THEME_LAST_USAGE_DATE, updated.lastCountedDate().toString());
            }
            if (updated.grandfatheredThemeId() != null) {
                editor.putString(PREFS_THEME_GRANDFATHERED_ID, updated.grandfatheredThemeId());
            }
            if (repairSelection) editor.putString(PREFS_SELECTED_THEME, selectedId);
            editor.apply();
        }
        return updated;
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
        try {
            return Integer.parseInt(mPrefs.getString(PREFS_NUMBER_OF_DECIMALS, mContext.getString(R.string.default_number_decimals)));
        } catch (NumberFormatException e) {
            return Integer.parseInt(mContext.getString(R.string.default_number_decimals));
        }
    }

    public String getDecimalSeparator() {
        return mPrefs.getString(PREFS_DECIMAL_SEPARATOR, mContext.getString(R.string.default_decimal_separator));
    }

    public String getGroupSeparator() {
        return mPrefs.getString(PREFS_GROUP_SEPARATOR, mContext.getString(R.string.default_group_separator));
    }

    public void setCurrencyLastUpdateDate(long timeInMilli) {
        mPrefs.edit().putLong(PREFS_CURR_LAST_UPDT, timeInMilli).apply();
    }

    public long getCurrencyLastUpdateDate() {
        return mPrefs.getLong(PREFS_CURR_LAST_UPDT,0);
    }

    public void setCurrencyResponse(String response) {
        mPrefs.edit().putString(PREFS_CURR_DATA, response).apply();
    }

    public String getCurrencyResponse() {
        return mPrefs.getString(PREFS_CURR_DATA, "");
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
