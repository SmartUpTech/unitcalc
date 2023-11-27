package net.smartlogic.unitconverter.activity;

import android.content.Intent;
import android.os.Bundle;

import net.smartlogic.unitconverter.BuildConfig;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.ThemeHelper;
import net.smartlogic.unitconverter.helper.Preferences;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        Preferences pref = Preferences.getInstance(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (pref.getPrefsTheme().equals(ThemeHelper.DARK_MODE)) {
                actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.action_bar_background));
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            final String shareBody = getString(R.string.note_share_body) + getString(R.string.url_app_short_link);

            Preference userButton = findPreference("share");
            userButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, "Share via"));
                    return true;
                }
            });

            SwitchPreference themePreference = findPreference(Preferences.PREFS_THEME);
            themePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    //Log.d("SHRIKI","Inside pref chnage:" + newValue.toString());
                    if ((Boolean) newValue) {
                        ThemeHelper.applyTheme(ThemeHelper.DARK_MODE);
                    }
                    else {
                        ThemeHelper.applyTheme(ThemeHelper.LIGHT_MODE);
                    }
                    return true;
                }
            });

            Preference version = findPreference("version");
            version.setSummary(BuildConfig.VERSION_NAME);
        }
    }
}