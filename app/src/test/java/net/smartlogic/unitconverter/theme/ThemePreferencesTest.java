package net.smartlogic.unitconverter.theme;

import android.app.Application;
import net.smartlogic.unitconverter.BuildConfig;
import android.content.SharedPreferences;
import net.smartlogic.unitconverter.helper.Preferences;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class ThemePreferencesTest {
    private Preferences preferences;
    private SharedPreferences store;
    private static final long NOW = 1_800_000_000_000L;
    @Before public void setup() {
        preferences = Preferences.getInstance(RuntimeEnvironment.getApplication());
        store = preferences.getPreferences();
        store.edit().clear().commit();
        ThemeManager.setProAccessProvider(() -> false);
    }
    @After public void cleanup() { ThemeManager.setProAccessProvider(() -> false); }
    private String reconcile(long now, boolean pro) { return preferences.reconcileThemeAccess(now, pro, true); }
    private void selected(String id) { store.edit().putString(Preferences.PREFS_SELECTED_THEME, id).commit(); }

    @Test public void freeSelectionsSurviveMigrationWithoutANoticeOrExpiry() {
        for (String id : new String[]{"porcelain_white", "oyster_cream", "titanium_gray"}) {
            store.edit().clear().putString(Preferences.PREFS_SELECTED_THEME, id).commit();
            assertEquals(id, reconcile(NOW, false));
            assertNull(preferences.consumeThemeNotice());
            assertEquals(0, preferences.getThemeExpiry(id));
        }
    }
    @Test public void freshInstallUsesBuildSpecificThemeAccess() {
        ThemeManager.init(RuntimeEnvironment.getApplication());
        assertNull(preferences.consumeThemeNotice());
        assertEquals(BuildConfig.DEBUG, preferences.setSelectedThemeId("glacier"));
        assertTrue(preferences.setSelectedThemeId("titanium_gray"));
        assertFalse(preferences.setSelectedThemeId("removed"));
    }
    @Test public void migrationRevokesProgressionAndGrandfatheredAccessWithoutGrace() {
        selected("glacier");
        store.edit().putInt("theme_usage_days", 1000).putString("theme_grandfathered_id", "glacier").commit();
        assertEquals(BuildConfig.DEBUG ? "glacier" : "porcelain_white", reconcile(NOW, false));
        assertEquals(BuildConfig.DEBUG ? null : Preferences.NOTICE_MIGRATED, preferences.consumeThemeNotice());
        assertNull(preferences.consumeThemeNotice());
        assertEquals(0, preferences.getThemeExpiry("glacier"));
        assertFalse(store.contains("theme_grandfathered_id"));
    }
    @Test public void proMigrationKeepsSelectionAndRewardExpiriesSurviveProLoss() {
        selected("glacier");
        assertEquals("glacier", reconcile(NOW, true));
        assertNull(preferences.consumeThemeNotice());
        assertTrue(preferences.saveThemeReward("sage_green", "sage", NOW));
        reconcile(NOW, true);
        selected("glacier");
        assertEquals(BuildConfig.DEBUG ? "glacier" : "porcelain_white", reconcile(NOW + 1, false));
        assertTrue(preferences.isThemeAvailable("sage_green", NOW + 1, false));
        assertEquals(BuildConfig.DEBUG ? null : Preferences.NOTICE_EXPIRED, preferences.consumeThemeNotice());
    }
    @Test public void independentRewardsDoNotRenewStackOrPauseWhenSwitching() {
        reconcile(NOW, false);
        assertTrue(preferences.saveThemeReward("glacier", "a", NOW));
        reconcile(NOW, false);
        assertTrue(preferences.saveThemeReward("sage_green", "b", NOW + 1000));
        reconcile(NOW + 1000, false);
        long expiry = NOW + ThemeAccess.REWARD_DURATION_MILLIS;
        assertEquals(expiry, preferences.getThemeExpiry("glacier"));
        assertEquals(expiry + 1000, preferences.getThemeExpiry("sage_green"));
        assertFalse(preferences.saveThemeReward("glacier", "renew-too-early", NOW + 2000));
        selected("porcelain_white");
        assertEquals("porcelain_white", reconcile(expiry, false));
        assertNull(preferences.consumeThemeNotice());
        assertEquals(BuildConfig.DEBUG, preferences.isThemeAvailable("glacier", expiry, false));
        assertTrue(preferences.isThemeAvailable("sage_green", expiry, false));
        assertTrue(preferences.saveThemeReward("glacier", "new-period", expiry));
        assertEquals(expiry + ThemeAccess.REWARD_DURATION_MILLIS, preferences.getThemeExpiry("glacier"));
    }
    @Test public void duplicateRewardCannotExtendAccessEvenAfterExpiry() {
        assertTrue(preferences.saveThemeReward("glacier", "a", NOW));
        assertFalse(preferences.saveThemeReward("glacier", "a", NOW + ThemeAccess.REWARD_DURATION_MILLIS + 1));
        assertEquals(NOW + ThemeAccess.REWARD_DURATION_MILLIS, preferences.getThemeExpiry("glacier"));
    }
    @Test public void rewardIsSavedBeforeSelectionAndRecoveredAfterRestart() {
        reconcile(NOW, false);
        selected("oyster_cream");
        assertTrue(preferences.saveThemeReward("glacier", "a", NOW));
        assertEquals("oyster_cream", preferences.getSelectedThemeId());
        assertEquals("glacier", store.getString(Preferences.PREFS_PENDING_REWARDED_THEME, null));
        assertEquals("glacier", reconcile(NOW + 1, false));
        assertEquals("glacier", preferences.getSelectedThemeId());
        assertFalse(store.contains(Preferences.PREFS_PENDING_REWARDED_THEME));
    }
    @Test public void pendingRewardWaitsForAdCloseAndOverridesQueuedFallback() {
        reconcile(NOW, false);
        store.edit().putString(Preferences.PREFS_THEME_NOTICE, Preferences.NOTICE_EXPIRED).commit();
        assertTrue(preferences.saveThemeReward("glacier", "a", NOW));
        assertEquals("porcelain_white", preferences.reconcileThemeAccess(NOW, false, false));
        assertEquals("glacier", reconcile(NOW, false));
        assertNull(preferences.consumeThemeNotice());
    }
    @Test public void expiredSelectedThemeFallsBackOnceWithoutSwitchingToAnotherUnlock() {
        reconcile(NOW, false);
        preferences.saveThemeReward("glacier", "a", NOW);
        reconcile(NOW, false);
        preferences.saveThemeReward("sage_green", "b", NOW + 1000);
        reconcile(NOW + 1000, false);
        selected("glacier");
        assertEquals(BuildConfig.DEBUG ? "glacier" : "porcelain_white", reconcile(NOW + ThemeAccess.REWARD_DURATION_MILLIS, false));
        assertTrue(preferences.isThemeAvailable("sage_green", NOW + ThemeAccess.REWARD_DURATION_MILLIS, false));
        assertEquals(BuildConfig.DEBUG ? null : Preferences.NOTICE_EXPIRED, preferences.consumeThemeNotice());
        reconcile(NOW + ThemeAccess.REWARD_DURATION_MILLIS, false);
        assertNull(preferences.consumeThemeNotice());
    }
    @Test public void explicitSelectionClearsNoticeEvenWhenAlreadyVisible() {
        ThemeManager.init(RuntimeEnvironment.getApplication());
        store.edit().putString(Preferences.PREFS_THEME_NOTICE, Preferences.NOTICE_EXPIRED).commit();
        ThemeManager.select(RuntimeEnvironment.getApplication(), "porcelain_white");
        assertNull(preferences.consumeThemeNotice());
    }
    @Test public void tappingExpiredAlreadyVisibleThemeRechecksAccess() {
        long now = System.currentTimeMillis();
        reconcile(now, false);
        preferences.saveThemeReward("glacier", "a", now);
        ThemeManager.init(RuntimeEnvironment.getApplication());
        store.edit().putLong(Preferences.PREFS_THEME_EXPIRY_PREFIX + "glacier", now - 1).commit();
        ThemeManager.select(RuntimeEnvironment.getApplication(), "glacier");
        assertEquals(BuildConfig.DEBUG ? "glacier" : "porcelain_white", ThemeManager.get().id());
        assertEquals(BuildConfig.DEBUG ? null : Preferences.NOTICE_EXPIRED, preferences.consumeThemeNotice());
    }
    @Test public void corruptAndRemovedIdsFallbackEvenForProAndInvalidPendingIsDiscarded() {
        for (boolean pro : new boolean[]{false, true}) {
            selected("removed");
            assertEquals("porcelain_white", reconcile(NOW, pro));
            store.edit().putInt(Preferences.PREFS_SELECTED_THEME, 42)
                    .putString(Preferences.PREFS_PENDING_REWARDED_THEME, "removed").commit();
            assertEquals("porcelain_white", reconcile(NOW, pro));
            assertFalse(store.contains(Preferences.PREFS_PENDING_REWARDED_THEME));
        }
        store.edit().putString(Preferences.PREFS_THEME_EXPIRY_PREFIX + "glacier", "bad").commit();
        assertEquals(BuildConfig.DEBUG, preferences.isThemeAvailable("glacier", NOW, false));
    }
    @Test public void invalidRewardsCannotUnlockFreeOrUnknownThemes() {
        assertFalse(preferences.saveThemeReward("porcelain_white", "a", NOW));
        assertFalse(preferences.saveThemeReward("removed", "a", NOW));
        assertFalse(preferences.saveThemeReward("glacier", "", NOW));
        assertFalse(preferences.saveThemeReward("glacier", "a", Long.MAX_VALUE));
    }
    @Test public void debugAccessDoesNotExpireOrGrantProOrWriteRewards() {
        org.junit.Assume.assumeTrue(BuildConfig.DEBUG);
        for (var theme : CalculatorThemes.all()) {
            assertTrue(preferences.setSelectedThemeId(theme.id()));
            assertEquals(theme.id(), reconcile(Long.MAX_VALUE, false));
            assertTrue(preferences.isThemeAvailable(theme.id(), Long.MAX_VALUE, false));
            assertEquals(0L, preferences.getThemeExpiry(theme.id()));
        }
        assertFalse(ThemeManager.hasProAccess());
        assertFalse(preferences.isThemeAvailable("removed", Long.MAX_VALUE, true));
        assertNull(preferences.consumeThemeNotice());
    }
}
