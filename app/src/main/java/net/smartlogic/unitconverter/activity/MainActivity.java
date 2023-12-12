package net.smartlogic.unitconverter.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.fragment.ConverterFragment;
import net.smartlogic.unitconverter.fragment.CurrencyConverterFragment;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.ThemeHelper;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    private BottomNavigationView bottomNavigationView;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Preferences pref = Preferences.getInstance(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (pref.getPrefsTheme().equals(ThemeHelper.DARK_MODE))
                actionBar.setBackgroundDrawable(getDrawable(R.drawable.action_bar_background));
        }
        context = this;

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);
        Preferences.getInstance(this).getPreferences().registerOnSharedPreferenceChangeListener(this);

        setUpBottomNavigation();
    }

    public void setUpBottomNavigation() {

        bottomNavigationView = findViewById(R.id.navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {

            Fragment selectedFragment;


            int itemId = item.getItemId();
            if (itemId == R.id.currency_converter) {
                AppConst.CURRENT_TAG = AppConst.TAG_CURRENCY;
                selectedFragment = CurrencyConverterFragment.newInstance();
                changeFragment(selectedFragment);
            } else if (itemId == R.id.settings) {
                Intent settingsActivity = new Intent(context, SettingsActivity.class);
                startActivity(settingsActivity);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return false;
            } else {
                AppConst.CURRENT_TAG = AppConst.TAG_UNIT;
                selectedFragment = ConverterFragment.newInstance();
                changeFragment(selectedFragment);
            }
            return true;
        });

    }

    private void changeFragment(Fragment selectedFragment) {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        transaction.replace(R.id.frame_layout, selectedFragment);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Preferences.getInstance(this).getPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //Log.d("SHRIKI", "Shared Pref changed for key:" + key);

        if (Preferences.getInstance(this).isLightTheme()) {
            setTheme(R.style.AppBaseTheme_Light);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

       /* if (id == R.id.menu_settings) {
            Intent settingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(settingsActivity);
        }*/
        if (id == R.id.menu_other_apps) {
            Intent launchActivity = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_other_apps)));
            startActivity(launchActivity);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostResume() {

        switch (AppConst.CURRENT_TAG) {
            case AppConst.TAG_CURRENCY:
                AppConst.CURRENT_TAG = AppConst.TAG_CURRENCY;
                bottomNavigationView.setSelectedItemId(R.id.currency_converter);
                break;
            case AppConst.TAG_UNIT:
            default:
                AppConst.CURRENT_TAG = AppConst.TAG_UNIT;
                bottomNavigationView.setSelectedItemId(R.id.unit_converter);
                break;
        }

        super.onPostResume();
    }
}
