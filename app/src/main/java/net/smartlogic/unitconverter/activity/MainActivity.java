package net.smartlogic.unitconverter.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
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
import net.smartlogic.unitconverter.fragment.HistoryFragment;
import net.smartlogic.unitconverter.games.GamesConfig;
import net.smartlogic.unitconverter.games.GamesFragment;
import net.smartlogic.unitconverter.helper.FavoritesRepository;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculationHistoryItem;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.theme.WindowChrome;
import net.smartlogic.unitconverter.timer.TimerFragment;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    public static final String EXTRA_OPEN_TIMER = "open_timer";
    private static final long EXIT_BACK_WINDOW_MS = 2000L;
    private long lastExitBackAt = -1L;
    private android.widget.Toast exitToast;
    private static final String SAVED_TAB_TAG = "main_tab_tag";
    private static final String SAVED_HISTORY_RETURN = "history_return_tab";
    private int historyReturnTab = R.id.calculator;

    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Context context;
    private final SparseArray<Fragment> tabFragments = new SparseArray<>();
    private Fragment activeFragment;
    private boolean initialTabSelected;
    private FavoritesRepository favoritesRepository;
    private String activeCalculatorId = CalculatorCatalog.ID_BASIC;
    private Fragment timerFragment;
    private boolean timerVisible;
    private final OnBackPressedCallback timerBackCallback = new OnBackPressedCallback(false) {
        @Override public void handleOnBackPressed() { hideTimerScreen(true); }
    };
    private final OnBackPressedCallback drawerBackCallback = new OnBackPressedCallback(true) {
        @Override public void handleOnBackPressed() { closeNavigationDrawerIfOpen(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) historyReturnTab = savedInstanceState.getInt(SAVED_HISTORY_RETURN, R.id.calculator);
        // Root fallback: later drawer, Timer and workspace callbacks keep priority.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (activeFragment instanceof HistoryFragment) { closeHistory(); return; }
                long now = android.os.SystemClock.elapsedRealtime();
                if (lastExitBackAt >= 0 && now - lastExitBackAt <= EXIT_BACK_WINDOW_MS) {
                    if (exitToast != null) exitToast.cancel();
                    finish();
                    return;
                }
                lastExitBackAt = now;
                if (exitToast != null) exitToast.cancel();
                exitToast = android.widget.Toast.makeText(MainActivity.this,
                        R.string.press_back_again_to_exit, android.widget.Toast.LENGTH_SHORT);
                exitToast.show();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.action_bar_brand);
            actionBar.setElevation(0f);
            View customView = actionBar.getCustomView();
            ImageButton menuButton = customView.findViewById(R.id.btn_nav_menu);
            if (menuButton != null) {
                menuButton.setOnClickListener(v -> {
                    if (activeFragment instanceof HistoryFragment) closeHistory();
                    else openDrawer();
                });
            }
        }
        WindowChrome.apply(this);
        context = this;
        favoritesRepository = FavoritesRepository.getInstance(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerOpened(@NonNull View drawerView) {
                lastExitBackAt = -1L;
                // Register when opened so the drawer takes precedence over fragment Back handlers.
                drawerBackCallback.remove();
                getOnBackPressedDispatcher().addCallback(MainActivity.this, drawerBackCallback);
            }

            @Override public void onDrawerClosed(@NonNull View drawerView) {
                drawerBackCallback.remove();
            }
        });
        navigationView = findViewById(R.id.nav_view);
        setupDrawerMenu();

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);
        Preferences.getInstance(this).getPreferences().registerOnSharedPreferenceChangeListener(this);

        restoreFragmentsIfNeeded(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, timerBackCallback);
        setUpBottomNavigation();
        net.smartlogic.unitconverter.helper.AdMobManager.getInstance(this).prepareGamesAds(this,
                () -> !isFinishing() && !isDestroyed(), this::invalidateOptionsMenu);
        ThemeApplier.applyNavigation(bottomNavigationView, navigationView);
        if (shouldOpenTimer(getIntent())) {
            showTimerScreen();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (shouldOpenTimer(intent)) {
            showTimerScreen();
        }
    }

    public boolean isTimerScreenVisible() { return timerVisible; }

    private boolean shouldOpenTimer(@Nullable Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_OPEN_TIMER, false);
    }

    private void setupDrawerMenu() {
        navigationView.getMenu().clear();
        int order = 0;
        for (CalculatorCatalog.Section section : CalculatorCatalog.getSections()) {
            android.view.SubMenu subMenu = navigationView.getMenu().addSubMenu(
                    getString(section.titleRes));
            for (CalculatorCatalog.Entry entry : section.entries) {
                if (entry.destination == CalculatorCatalog.Destination.EXPLORE
                        || entry.destination == CalculatorCatalog.Destination.FAVORITES) {
                    continue;
                }
                subMenu.add(Menu.NONE, entry.menuId, order++, getString(entry.titleRes))
                        .setIcon(entry.iconRes);
            }
        }
        navigationView.getMenu().add(Menu.NONE, R.id.menu_games_privacy, order, R.string.games_privacy_options)
                .setVisible(false);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_games_privacy) {
                net.smartlogic.unitconverter.helper.AdMobManager.getInstance(this).showGamesPrivacyOptions(this);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
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
            case UNIT_CONVERTER:
                selectTab(R.id.converter);
                runOnConverterFragment(ConverterFragment::openUnitMode);
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
                selectTab(R.id.history);
                break;
            case TIMER:
                showTimerScreen();
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

    public boolean closeNavigationDrawerIfOpen() {
        if (drawerLayout == null || !drawerLayout.isDrawerOpen(GravityCompat.START)) return false;
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void restoreFromHistory(@NonNull CalculationHistoryItem item) {
        CalculatorCatalog.Entry entry = CalculatorCatalog.getById(item.calculatorId());
        if (entry == null) {
            entry = CalculatorCatalog.getById(CalculatorCatalog.ID_BASIC);
        }
        if (entry == null) {
            return;
        }

        final CalculatorCatalog.Entry targetEntry = entry;
        switch (targetEntry.destination) {
            case BASIC_CALCULATOR:
                selectTab(R.id.calculator);
                runOnCalculatorFragment(fragment -> fragment.restoreFromHistory(item));
                break;
            case UNIT_CATEGORY:
                selectTab(R.id.converter);
                runOnConverterFragment(fragment -> {
                    fragment.openConvertWorkspace();
                    fragment.openUnitCategory(targetEntry.unitCategoryId);
                });
                break;
            case CURRENCY:
                selectTab(R.id.converter);
                runOnConverterFragment(fragment -> {
                    fragment.openConvertWorkspace();
                    fragment.openCurrencyMode();
                });
                break;
            default:
                selectTab(R.id.calculator);
                runOnCalculatorFragment(fragment -> fragment.restoreFromHistory(item));
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

    private void runOnConverterFragment(@NonNull ConverterFragment.ReadyAction action) {
        Runnable task = () -> {
            Fragment fragment = tabFragments.get(R.id.converter);
            if (fragment instanceof ConverterFragment) {
                ((ConverterFragment) fragment).runWhenReady(action);
            }
        };

        Fragment fragment = tabFragments.get(R.id.converter);
        if (fragment instanceof ConverterFragment) {
            task.run();
            return;
        }

        selectTab(R.id.converter);
        findViewById(R.id.frame_layout).post(task);
    }

    private void runOnCalculatorFragment(@NonNull CalculatorAction action) {
        Runnable task = () -> {
            Fragment fragment = tabFragments.get(R.id.calculator);
            if (fragment instanceof CalculatorFragment) {
                action.run((CalculatorFragment) fragment);
            }
        };

        Fragment fragment = tabFragments.get(R.id.calculator);
        if (fragment instanceof CalculatorFragment) {
            task.run();
            return;
        }

        selectTab(R.id.calculator);
        findViewById(R.id.frame_layout).post(task);
    }

    private interface CalculatorAction {
        void run(@NonNull CalculatorFragment fragment);
    }

    private void restoreFragmentsIfNeeded(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        AppConst.CURRENT_TAG = savedInstanceState.getString(SAVED_TAB_TAG, AppConst.CURRENT_TAG);
        registerRestoredFragment(R.id.calculator, AppConst.TAG_CALC);
        registerRestoredFragment(R.id.converter, AppConst.TAG_CONVERTER);
        registerRestoredFragment(R.id.history, AppConst.TAG_HISTORY);
        registerRestoredFragment(R.id.explore, AppConst.TAG_EXPLORE);
        registerRestoredFragment(R.id.favorites, AppConst.TAG_FAVORITES);
        registerRestoredFragment(R.id.play, GamesConfig.TAG);
        Fragment restoredTimer = getSupportFragmentManager().findFragmentByTag(AppConst.TAG_TIMER);
        if (restoredTimer != null) {
            timerFragment = restoredTimer;
            timerVisible = !restoredTimer.isHidden();
            timerBackCallback.setEnabled(timerVisible);
            if (timerVisible) {
                activeFragment = restoredTimer;
            }
        }
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
            int restoredId = resolveMenuIdForTag(AppConst.CURRENT_TAG);
            if (restoredId == R.id.history) selectTab(restoredId);
            else bottomNavigationView.setSelectedItemId(restoredId);
        }
    }

    private void selectTab(int itemId) {
        selectTab(itemId, true);
    }

    private void selectTab(int itemId, boolean openWorkspace) {
        lastExitBackAt = -1L;
        if (itemId == R.id.history && !(activeFragment instanceof HistoryFragment)) {
            int previous = resolveMenuIdForTag(AppConst.CURRENT_TAG);
            if (previous != R.id.history) historyReturnTab = previous;
        }
        Fragment fragment = tabFragments.get(itemId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (timerVisible && timerFragment != null) {
            // Hide Timer and reveal the selected tab in one transaction.
            transaction.hide(timerFragment);
            timerVisible = false;
            timerBackCallback.setEnabled(false);
        }

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
        if (openWorkspace && itemId == R.id.converter && fragment instanceof ConverterFragment) {
            ((ConverterFragment) fragment).openConvertWorkspace();
        }
        updateCurrentTag(itemId);
        updateHistoryChrome(itemId == R.id.history);
    }

    public void closeHistory() {
        selectTab(historyReturnTab == R.id.history ? R.id.calculator : historyReturnTab, false);
        refreshActiveCalculatorId();
        updateFavoriteMenuItem();
    }

    private void updateHistoryChrome(boolean history) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(history ? View.GONE : View.VISIBLE);
            MenuItem selected = bottomNavigationView.getMenu().findItem(resolveMenuIdForTag(AppConst.CURRENT_TAG));
            if (!history && selected != null) selected.setChecked(true);
        }
        android.widget.TextView title = findViewById(R.id.tv_app_bar_brand);
        ImageButton navigation = findViewById(R.id.btn_nav_menu);
        if (title != null) title.setText(history ? R.string.nav_history : R.string.app_bar_brand);
        if (navigation != null) {
            navigation.setImageResource(history ? R.drawable.ic_settings_back : R.drawable.ic_menu);
            navigation.setContentDescription(getString(history ? R.string.history_back : R.string.nav_open_drawer));
        }
        WindowChrome.apply(this);
        invalidateOptionsMenu();
    }

    @NonNull
    private Fragment createFragmentFor(int itemId) {
        if (itemId == R.id.play) return new GamesFragment();
        if (itemId == R.id.calculator) {
            return CalculatorFragment.newInstance();
        }
        if (itemId == R.id.converter) {
            return ConverterFragment.newInstance();
        }
        if (itemId == R.id.history) {
            return HistoryFragment.newInstance();
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
        if (itemId == R.id.play) {
            AppConst.CURRENT_TAG = GamesConfig.TAG;
            return;
        }
        if (itemId == R.id.calculator) {
            AppConst.CURRENT_TAG = AppConst.TAG_CALC;
        } else if (itemId == R.id.converter) {
            AppConst.CURRENT_TAG = AppConst.TAG_CONVERTER;
        } else if (itemId == R.id.history) {
            AppConst.CURRENT_TAG = AppConst.TAG_HISTORY;
        } else if (itemId == R.id.explore) {
            AppConst.CURRENT_TAG = AppConst.TAG_EXPLORE;
        } else if (itemId == R.id.favorites) {
            AppConst.CURRENT_TAG = AppConst.TAG_FAVORITES;
        }
    }

    private int resolveMenuIdForTag(@NonNull String tag) {
        if (GamesConfig.TAG.equals(tag)) return R.id.play;
        if (AppConst.TAG_CALC.equals(tag)) {
            return R.id.calculator;
        }
        if (AppConst.TAG_CONVERTER.equals(tag)) {
            return R.id.converter;
        }
        if (AppConst.TAG_HISTORY.equals(tag)) {
            return R.id.history;
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
        if (itemId == R.id.play) return GamesConfig.TAG;
        if (itemId == R.id.calculator) {
            return AppConst.TAG_CALC;
        }
        if (itemId == R.id.converter) {
            return AppConst.TAG_CONVERTER;
        }
        if (itemId == R.id.history) {
            return AppConst.TAG_HISTORY;
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
        if (timerVisible) {
            activeCalculatorId = CalculatorCatalog.ID_TIMER;
        } else if (activeFragment instanceof CalculatorFragment) {
            activeCalculatorId = CalculatorCatalog.ID_BASIC;
        } else if (activeFragment instanceof ConverterFragment) {
            activeCalculatorId = ((ConverterFragment) activeFragment).getActiveCatalogId();
        }
    }

    private void showTimerScreen() {
        lastExitBackAt = -1L;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (timerFragment == null) {
            timerFragment = getSupportFragmentManager().findFragmentByTag(AppConst.TAG_TIMER);
        }
        if (timerFragment == null) {
            timerFragment = TimerFragment.newInstance();
            if (activeFragment != null && !timerVisible) {
                transaction.hide(activeFragment);
            }
            transaction.add(R.id.frame_layout, timerFragment, AppConst.TAG_TIMER);
        } else if (!timerVisible) {
            if (activeFragment != null) {
                transaction.hide(activeFragment);
            }
            transaction.show(timerFragment);
        }
        transaction.commit();
        timerVisible = true;
        timerBackCallback.remove();
        getOnBackPressedDispatcher().addCallback(this, timerBackCallback);
        timerBackCallback.setEnabled(true);
        activeFragment = timerFragment;
        refreshActiveCalculatorId();
        updateFavoriteMenuItem();
    }

    private void hideTimerScreen(boolean restoreTab) {
        if (!timerVisible || timerFragment == null) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(timerFragment);
        if (restoreTab) {
            Fragment tabFragment = tabFragments.get(resolveMenuIdForTag(AppConst.CURRENT_TAG));
            if (tabFragment != null) {
                transaction.show(tabFragment);
                activeFragment = tabFragment;
            }
        }
        transaction.commit();
        timerVisible = false;
        timerBackCallback.setEnabled(false);
        refreshActiveCalculatorId();
        updateFavoriteMenuItem();
    }

    private void updateFavoriteMenuItem() {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        boolean history = activeFragment instanceof HistoryFragment;
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setVisible(item.getItemId() == (history ? R.id.menu_clear_history : R.id.menu_history));
            if (item.getItemId() == R.id.menu_clear_history) item.setEnabled(history && ((HistoryFragment) activeFragment).hasHistory());
            if (item.getIcon() != null) item.getIcon().setTint(ThemeManager.get().functions());
        }
        // Keep consent controls reachable from the drawer with a History-only app bar.
        MenuItem privacy = navigationView.getMenu().findItem(R.id.menu_games_privacy);
        if (privacy != null) privacy.setVisible(net.smartlogic.unitconverter.helper.AdMobManager
                .getInstance(this).isGamesPrivacyOptionsRequired());
        // AppCompat installs the menu presenter after the initial window chrome pass.
        WindowChrome.apply(this);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override protected void onStop() {
        lastExitBackAt = -1L;
        if (exitToast != null) exitToast.cancel();
        super.onStop();
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
        if (id == R.id.menu_clear_history && activeFragment instanceof HistoryFragment history) {
            history.showClearHistoryConfirmation();
            return true;
        }
        if (id == R.id.menu_history) {
            selectTab(R.id.history);
            return true;
        }
        if (id == R.id.menu_games_privacy) {
            net.smartlogic.unitconverter.helper.AdMobManager.getInstance(this).showGamesPrivacyOptions(this);
            return true;
        }
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
    protected void onResume() {
        super.onResume();
        WindowChrome.apply(this);
        ThemeApplier.applyNavigation(bottomNavigationView, navigationView);
        ThemeApplier.apply(findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onPostResume() {
        int currentId = resolveMenuIdForTag(AppConst.CURRENT_TAG);
        // A widget/notification may have opened Timer during onCreate.
        // Reselecting a tab here would immediately hide that screen.
        if (!timerVisible) {
            if (currentId == R.id.history) {
                if (!(activeFragment instanceof HistoryFragment)) selectTab(currentId);
            } else {
                bottomNavigationView.setSelectedItemId(currentId);
            }
        }
        refreshActiveCalculatorId();
        updateFavoriteMenuItem();
        super.onPostResume();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(SAVED_TAB_TAG, AppConst.CURRENT_TAG);
        outState.putInt(SAVED_HISTORY_RETURN, historyReturnTab);
        super.onSaveInstanceState(outState);
    }
}
