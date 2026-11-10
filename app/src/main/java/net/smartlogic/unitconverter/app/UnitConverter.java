package net.smartlogic.unitconverter.app;

import android.app.Application;

import net.smartlogic.unitconverter.helper.AdMobManager;
import net.smartlogic.unitconverter.helper.AppOpenManager;
import net.smartlogic.unitconverter.helper.ThemeHelper;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.theme.ThemeAccessLifecycle;

public class UnitConverter extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        boolean existingInstallation = !androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this).getAll().isEmpty();
        try {
            var info = getPackageManager().getPackageInfo(getPackageName(), 0);
            existingInstallation |= info.firstInstallTime != info.lastUpdateTime;
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
        net.smartlogic.unitconverter.helper.Preferences.getInstance(this)
                .migrateRegionalPreferences(existingInstallation);
        net.smartlogic.unitconverter.helper.UsageAnalytics.initialize(this);
        AdMobManager.getInstance(this);
        AppOpenManager.getInstance(this);

        ThemeHelper.lockResourceNightMode();
        ThemeManager.init(this);
        net.smartlogic.unitconverter.theme.AppearanceTypography.install(this);
        ThemeAccessLifecycle.install(this);
    }
}
