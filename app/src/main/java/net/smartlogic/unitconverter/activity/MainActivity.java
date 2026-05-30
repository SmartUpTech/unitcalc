package net.smartlogic.unitconverter.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.fragment.CalculatorFragment;
import net.smartlogic.unitconverter.fragment.CurrencyConverterFragment;
import net.smartlogic.unitconverter.fragment.UnitConverterFragment;
import net.smartlogic.unitconverter.helper.AdMobManager;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.ThemeHelper;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    private BottomNavigationView bottomNavigationView;
    private Context context;
    private int actionCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        Preferences pref = Preferences.getInstance(this);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorHeader));
        WindowInsetsControllerCompat wic = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Since colorHeader is dark in both light (#2d3e50) and dark (#2b2b2b) modes, 
        // we want light icons (AppearanceLightStatusBars = false)
        wic.setAppearanceLightStatusBars(false);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (pref.getPrefsTheme().equals(ThemeHelper.DARK_MODE)) {
                actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.action_bar_background));
            }
        }
        context = this;

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);
        Preferences.getInstance(this).getPreferences().registerOnSharedPreferenceChangeListener(this);

        //AdMobManager.getInstance(this).loadInterstitialAd();

        setUpBottomNavigation();
    }

    public void setUpBottomNavigation() {

        bottomNavigationView = findViewById(R.id.navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {

            Fragment selectedFragment;

//            actionCount++;
//            if (actionCount % 3 == 0) {
//                AdMobManager.getInstance(this).showInterstitialAd(this);
//                AdMobManager.getInstance(this).loadInterstitialAd();
//            }

            int itemId = item.getItemId();
            if (itemId == R.id.calculator) {
                AppConst.CURRENT_TAG = AppConst.TAG_CALC;
                selectedFragment = CalculatorFragment.newInstance();
                changeFragment(selectedFragment);
            } else if (itemId == R.id.currency_converter) {
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
                selectedFragment = UnitConverterFragment.newInstance();
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        // Preference changes are handled by AppCompatDelegate and recreation
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_other_apps) {
            Intent launchActivity = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_other_apps)));
            startActivity(launchActivity);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostResume() {

        switch (AppConst.CURRENT_TAG) {
            case AppConst.TAG_CALC:
                AppConst.CURRENT_TAG = AppConst.TAG_CALC;
                bottomNavigationView.setSelectedItemId(R.id.calculator);
                break;
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
