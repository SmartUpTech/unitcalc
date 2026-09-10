package net.smartlogic.unitconverter.app;

import android.app.Application;

import net.smartlogic.unitconverter.helper.AdMobManager;
import net.smartlogic.unitconverter.helper.AppOpenManager;
import net.smartlogic.unitconverter.helper.ThemeHelper;
import net.smartlogic.unitconverter.theme.ThemeManager;

public class UnitConverter extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AdMobManager.getInstance(this);
        AppOpenManager.getInstance(this);

        ThemeHelper.lockResourceNightMode();
        ThemeManager.init(this);
    }
}
