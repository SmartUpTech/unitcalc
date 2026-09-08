package net.smartlogic.unitconverter.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.fragment.CalculatorFragment;
import net.smartlogic.unitconverter.fragment.ConverterFragment;
import net.smartlogic.unitconverter.fragment.ExploreFragment;
import net.smartlogic.unitconverter.fragment.FavoritesFragment;
import net.smartlogic.unitconverter.helper.FavoritesRepository;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculatorCatalog;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Context context;
    private final SparseArray<Fragment> tabFragments = new SparseArray<>();
    private Fragment activeFragment;
    private boolean initialTabSelected;
    private FavoritesRepository favoritesRepository;
    private String activeCalculatorId = CalculatorCatalog.ID_BASIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.chrome_surface));
        WindowInsetsControllerCompat wic = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        wic.setAppearanceLightStatusBars(!night);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.action_bar_brand);
            actionBar.setElevation(0f);
            actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.action_bar_background));
            View customView = actionBar.getCustomView();
            ImageButton menuButton = customView.findViewById(R.id.btn_nav_menu);
            if (menuButton != null) {
                menuButton.setOnClickListener(v -> openDrawer());
            }
        }
        context = this;
        favoritesRepository = FavoritesRepository.getInstance(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        setupDrawerMenu();

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);
        Preferences.getInstance(this).getPreferences().registerOnSharedPreferenceChangeListener(this);

        restoreFragmentsIfNeeded(savedInstanceState);
        setUpBottomNavigation();
    }

    private void setupDrawerMenu() {
        navigationView.getMenu().clear();
        int order = 0;
        for (CalculatorCatalog.Section section : CalculatorCatalog.getSections()) {
            android.view.SubMenu subMenu = navigationView.getMenu().addSubMenu(
                    getString(section.titleRes));
            for (CalculatorCatalog.Entry entry : section.entries) {
                subMenu.add(Menu.NONE, entry.menuId, order++, getString(entry.titleRes))
                        .setIcon(entry.iconRes);
            }
        }
        navigationView.setNavigationItemSelectedListener(item -> {
            CalculatorCatalog.Entry entry = CalculatorCatalog.getByMenuId(item.getItemId());
            if (entry != null) {
                navigateToEntry(entry);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void navigateToEntry(@NonNull CalculatorCatalog.Entry entry) {
        switch (entry.destination) {
            case BASIC_CALCULATOR:
                selectTab(R.id.calculator);
                break;
            case UNIT_CATEGORY:
                selectTab(R.id.converter);
                runOnConverterFragment(fragment -> fragment.openUnitCategory(entry.unitCategoryId));
                break;
            case CURRENCY:
                selectTab(R.id.converter);
                runOnConverterFragment(ConverterFragment::openCurrencyMode);
                break;
            case EXPLORE:
                selectTab(R.id.explore);
                break;
            case FAVORITES:
                selectTab(R.id.favorites);
                break;
            case SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case HISTORY:
                selectTab(R.id.calculator);
                runOnCalculatorFragment(CalculatorFragment::openHistoryTab);
                break;
            case RATE_APP:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_app_link))));
                break;
            case SHARE_APP:
                shareApp();
                break;
            default:
                break;
        }
        refreshActiveCalculatorId();
        updateFavoriteMenuItem();
    }

    private void shareApp() {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.brand_name));
        sharingIntent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.note_share_body) + getString(R.string.url_app_short_link));
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)));
    }

    private void runOnConverterFragment(@NonNull ConverterAction action) {
        Fragment fragment = tabFragments.get(R.id.converter);
        if (fragment instanceof ConverterFragment) {
            action.run((ConverterFragment) fragment);
        } else {
            findViewById(R.id.frame_layout).post(() -> {
                Fragment loaded = tabFragments.get(R.id.converter);
                if (loaded instanceof ConverterFragment) {
                    action.run((ConverterFragment) loaded);
                }
            });
        }
    }

    private void runOnCalculatorFragment(@NonNull CalculatorAction action) {
        Fragment fragment = tabFragments.get(R.id.calculator);
        if (fragment instanceof CalculatorFragment) {
            action.run((CalculatorFragment) fragment);
        } else {
            findViewById(R.id.frame_layout).post(() -> {
                Fragment loaded = tabFragments.get(R.id.calculator);
                if (loaded instanceof CalculatorFragment) {
                    action.run((CalculatorFragment) loaded);
                }
            });
        }
    }

    private interface ConverterAction {
        void run(@NonNull ConverterFragment fragment);
    }

    private interface CalculatorAction {
        void run(@NonNull CalculatorFragment fragment);
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
            refreshActiveCalculatorId();
            updateFavoriteMenuItem();
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

    private void refreshActiveCalculatorId() {
        if (activeFragment instanceof CalculatorFragment) {
            activeCalculatorId = CalculatorCatalog.ID_BASIC;
        } else if (activeFragment instanceof ConverterFragment) {
            activeCalculatorId = ((ConverterFragment) activeFragment).getActiveCatalogId();
        }
    }

    private void updateFavoriteMenuItem() {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem favoriteItem = menu.findItem(R.id.menu_favorite);
        if (favoriteItem != null) {
            CalculatorCatalog.Entry entry = CalculatorCatalog.getById(activeCalculatorId);
            boolean showFavorite = entry != null && entry.isFavoriteEligible();
            favoriteItem.setVisible(showFavorite);
            if (showFavorite) {
                boolean isFavorite = favoritesRepository.isFavorite(activeCalculatorId);
                favoriteItem.setIcon(R.drawable.ic_favorite);
                if (isFavorite) {
                    favoriteItem.getIcon().setTint(ContextCompat.getColor(this, R.color.accent));
                } else {
                    favoriteItem.getIcon().setTint(ContextCompat.getColor(this, R.color.screen_expression_text));
                }
            }
        }
        MenuItem settingsItem = menu.findItem(R.id.menu_settings);
        if (settingsItem != null && settingsItem.getIcon() != null) {
            settingsItem.getIcon().setTint(ContextCompat.getColor(this, R.color.screen_expression_text));
        }
        return super.onPrepareOptionsMenu(menu);
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
        if (id == R.id.menu_favorite) {
            CalculatorCatalog.Entry entry = CalculatorCatalog.getById(activeCalculatorId);
            if (entry != null && entry.isFavoriteEligible()) {
                favoritesRepository.toggleFavorite(activeCalculatorId);
                updateFavoriteMenuItem();
            }
            return true;
        }
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
        refreshActiveCalculatorId();
        updateFavoriteMenuItem();
        super.onPostResume();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }
}
