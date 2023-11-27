package net.smartlogic.unitconverter.app;

import android.app.Application;

import net.smartlogic.unitconverter.helper.AdMobManager;
import net.smartlogic.unitconverter.helper.AppOpenManager;
import net.smartlogic.unitconverter.helper.ThemeHelper;
import net.smartlogic.unitconverter.helper.Preferences;

public class UnitConverter extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AdMobManager.getInstance(this);
        AppOpenManager.getInstance(this);

        Preferences pref = Preferences.getInstance(this);
        ThemeHelper.applyTheme(pref.getPrefsTheme());
    }
}
