package net.smartlogic.unitconverter.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.fragment.CalculatorFragment;
import net.smartlogic.unitconverter.fragment.ConverterFragment;
import net.smartlogic.unitconverter.fragment.ExploreFragment;
import net.smartlogic.unitconverter.fragment.FavoritesFragment;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.ThemeHelper;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    private BottomNavigationView bottomNavigationView;
    private Context context;
    private final SparseArray<Fragment> tabFragments = new SparseArray<>();
    private Fragment activeFragment;
    private boolean initialTabSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        Preferences pref = Preferences.getInstance(this);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.app_bar));
        WindowInsetsControllerCompat wic = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        wic.setAppearanceLightStatusBars(!night);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.action_bar_background));
        }
        context = this;

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);
        Preferences.getInstance(this).getPreferences().registerOnSharedPreferenceChangeListener(this);

        restoreFragmentsIfNeeded(savedInstanceState);
        setUpBottomNavigation();
    }

    private void restoreFragmentsIfNeeded(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        registerRestoredFragment(R.id.calculator, AppConst.TAG_CALC);
        registerRestoredFragment(R.id.converter, AppConst.TAG_CONVERTER);
        registerRestoredFragment(R.id.explore, AppConst.TAG_EXPLORE);
        registerRestoredFragment(R.id.favorites, AppConst.TAG_FAVORITES);
    }

    private void registerRestoredFragment(int menuId, String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            tabFragments.put(menuId, fragment);
            if (!fragment.isHidden()) {
                activeFragment = fragment;
            }
        }
    }

    private void setUpBottomNavigation() {
        bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            selectTab(item.getItemId());
            return true;
        });

        if (!initialTabSelected) {
            initialTabSelected = true;
            bottomNavigationView.setSelectedItemId(resolveMenuIdForTag(AppConst.CURRENT_TAG));
        }
    }

    private void selectTab(int itemId) {
        Fragment fragment = tabFragments.get(itemId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (fragment == null) {
            fragment = createFragmentFor(itemId);
            if (activeFragment == null) {
                transaction.add(R.id.frame_layout, fragment, fragmentTagFor(itemId));
            } else {
                transaction.hide(activeFragment)
                        .add(R.id.frame_layout, fragment, fragmentTagFor(itemId));
            }
            tabFragments.put(itemId, fragment);
        } else if (fragment != activeFragment) {
            transaction.hide(activeFragment).show(fragment);
        }

        transaction.commit();
        activeFragment = fragment;
        updateCurrentTag(itemId);
    }

    @NonNull
    private Fragment createFragmentFor(int itemId) {
        if (itemId == R.id.calculator) {
            return CalculatorFragment.newInstance();
        }
        if (itemId == R.id.converter) {
            return ConverterFragment.newInstance();
        }
        if (itemId == R.id.explore) {
            return ExploreFragment.newInstance();
        }
        if (itemId == R.id.favorites) {
            return FavoritesFragment.newInstance();
        }
        return CalculatorFragment.newInstance();
    }

    private void updateCurrentTag(int itemId) {
        if (itemId == R.id.calculator) {
            AppConst.CURRENT_TAG = AppConst.TAG_CALC;
        } else if (itemId == R.id.converter) {
            AppConst.CURRENT_TAG = AppConst.TAG_CONVERTER;
        } else if (itemId == R.id.explore) {
            AppConst.CURRENT_TAG = AppConst.TAG_EXPLORE;
        } else if (itemId == R.id.favorites) {
            AppConst.CURRENT_TAG = AppConst.TAG_FAVORITES;
        }
    }

    private int resolveMenuIdForTag(@NonNull String tag) {
        if (AppConst.TAG_CALC.equals(tag)) {
            return R.id.calculator;
        }
        if (AppConst.TAG_CONVERTER.equals(tag)) {
            return R.id.converter;
        }
        if (AppConst.TAG_EXPLORE.equals(tag)) {
            return R.id.explore;
        }
        if (AppConst.TAG_FAVORITES.equals(tag)) {
            return R.id.favorites;
        }
        return R.id.calculator;
    }

    @NonNull
    private String fragmentTagFor(int itemId) {
        if (itemId == R.id.calculator) {
            return AppConst.TAG_CALC;
        }
        if (itemId == R.id.converter) {
            return AppConst.TAG_CONVERTER;
        }
        if (itemId == R.id.explore) {
            return AppConst.TAG_EXPLORE;
        }
        if (itemId == R.id.favorites) {
            return AppConst.TAG_FAVORITES;
        }
        return AppConst.TAG_CALC;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Preferences.getInstance(this).getPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
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
            return true;
        }
        if (id == R.id.menu_settings) {
            Intent settingsActivity = new Intent(context, SettingsActivity.class);
            startActivity(settingsActivity);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostResume() {
        bottomNavigationView.setSelectedItemId(resolveMenuIdForTag(AppConst.CURRENT_TAG));
        super.onPostResume();
    }
}
