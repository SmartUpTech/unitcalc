package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import net.smartlogic.unitconverter.theme.ThemeAccess;
import net.smartlogic.unitconverter.theme.ThemeManager;

import java.util.Map;

public class Preferences {

    public static final String PREFS_SELECTED_THEME = "selected_theme";
    public static final String PREFS_THEME_EXPIRY_PREFIX = "theme_expiry_";
    public static final String PREFS_PENDING_REWARDED_THEME = "pending_rewarded_theme";
    public static final String PREFS_THEME_ACCESS_VERSION = "theme_access_version";
    public static final String PREFS_THEME_NOTICE = "theme_access_notice";
    private static final String REWARD_ATTEMPT_PREFIX = "theme_reward_attempt_";
    public static final String NOTICE_EXPIRED = "expired";
    public static final String NOTICE_MIGRATED = "migrated";
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

    private static final String PREFS_BMI_STATE = "bmi_state_v1";
    public String getBmiState() {
        try { return mPrefs.getString(PREFS_BMI_STATE, ""); }
        catch (ClassCastException invalid) { return ""; }
    }
    public void setBmiState(String state) { mPrefs.edit().putString(PREFS_BMI_STATE, state).apply(); }

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
        long now = System.currentTimeMillis();
        reconcileThemeAccess(now, ThemeManager.hasProAccess(),
                !net.smartlogic.unitconverter.theme.ThemeRewardCoordinator.isBusy());
        if (!isThemeAvailable(themeId, now, ThemeManager.hasProAccess())) return false;
        // Clear a stale notice even when explicitly selecting the already-visible theme.
        mPrefs.edit().putString(PREFS_SELECTED_THEME, themeId)
                .remove(PREFS_THEME_NOTICE).remove(PREFS_PENDING_REWARDED_THEME).apply();
        return true;
    }

    public long getThemeExpiry(String themeId) {
        Object value = mPrefs.getAll().get(PREFS_THEME_EXPIRY_PREFIX + themeId);
        return value instanceof Long ? Math.max(0L, (Long) value) : 0L;
    }

    public boolean isThemeAvailable(String themeId, long now, boolean pro) {
        return ThemeAccess.isAvailable(themeId, pro, getThemeExpiry(themeId), now);
    }

    /** Startup/foreground reconciliation. No timer mutates the visible theme. */
    public synchronized String reconcileThemeAccess(long now, boolean pro, boolean finishPending) {
        Map<String, ?> values = mPrefs.getAll();
        Object rawSelected = values.get(PREFS_SELECTED_THEME);
        String selected = getSelectedThemeId();
        Object version = values.get(PREFS_THEME_ACCESS_VERSION);
        boolean migration = !(version instanceof Integer) || (Integer) version < ThemeAccess.VERSION;
        SharedPreferences.Editor editor = mPrefs.edit();
        boolean changed = false;
        if (migration) {
            editor.putInt(PREFS_THEME_ACCESS_VERSION, ThemeAccess.VERSION)
                    .remove("theme_usage_days").remove("theme_last_usage_date")
                    .remove("theme_progression_version").remove("theme_grandfathered_id");
            changed = true;
        }
        Object pending = values.get(PREFS_PENDING_REWARDED_THEME);
        // Pending must represent a real, unexpired earned reward, not merely Pro access.
        if (finishPending && pending != null) {
            if (pending instanceof String && ThemeAccess.isKnown((String) pending)
                    && getThemeExpiry((String) pending) > now) {
                selected = (String) pending;
                editor.putString(PREFS_SELECTED_THEME, selected).remove(PREFS_THEME_NOTICE);
            }
            editor.remove(PREFS_PENDING_REWARDED_THEME);
            changed = true;
        }
        if (!isThemeAvailable(selected, now, pro)) {
            selected = CalculatorThemes.DEFAULT_ID;
            editor.putString(PREFS_SELECTED_THEME, selected)
                    .putString(PREFS_THEME_NOTICE, migration ? NOTICE_MIGRATED : NOTICE_EXPIRED);
            changed = true;
        } else if (rawSelected != null && !ThemeAccess.isKnown(rawSelected instanceof String ? (String) rawSelected : null)) {
            editor.putString(PREFS_SELECTED_THEME, selected);
            changed = true;
        }
        if (changed) editor.apply();
        return selected;
    }

    /** Call on the serialized reward executor. Commit completes before UI recovery. */
    public boolean saveThemeReward(String themeId, String attemptId, long rewardTime) {
        if (!ThemeAccess.isKnown(themeId) || ThemeAccess.isAlwaysFree(themeId)
                || attemptId == null || attemptId.isEmpty() || rewardTime < 0
                || rewardTime > Long.MAX_VALUE - ThemeAccess.REWARD_DURATION_MILLIS) return false;
        Map<String, ?> values = mPrefs.getAll();
        if (attemptId.equals(values.get(REWARD_ATTEMPT_PREFIX + themeId))) return false;
        if (getThemeExpiry(themeId) > rewardTime) return false;
        return mPrefs.edit()
                .putLong(PREFS_THEME_EXPIRY_PREFIX + themeId, rewardTime + ThemeAccess.REWARD_DURATION_MILLIS)
                .putString(REWARD_ATTEMPT_PREFIX + themeId, attemptId)
                .putString(PREFS_PENDING_REWARDED_THEME, themeId).commit();
    }

    public synchronized String consumeThemeNotice() {
        Object notice = mPrefs.getAll().get(PREFS_THEME_NOTICE);
        if (notice != null) mPrefs.edit().remove(PREFS_THEME_NOTICE).apply();
        return notice instanceof String ? (String) notice : null;
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

    public void setCurrencySnapshot(String response, long fetchedAt) {
        mPrefs.edit().putString(PREFS_CURR_DATA, response)
                .putLong(PREFS_CURR_LAST_UPDT, fetchedAt).apply();
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
