package net.smartlogic.unitconverter.app;

import android.app.Application;

import net.smartlogic.unitconverter.helper.ThemeHelper;
import net.smartlogic.unitconverter.helper.Preferences;

public class UnitConverter extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Preferences pref = Preferences.getInstance(this);
        ThemeHelper.applyTheme(pref.getPrefsTheme());
    }
}
