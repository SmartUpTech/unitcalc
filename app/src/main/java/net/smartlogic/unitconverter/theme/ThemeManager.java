package net.smartlogic.unitconverter.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.smartlogic.unitconverter.helper.Preferences;

import java.util.concurrent.CopyOnWriteArrayList;

public final class ThemeManager {

    public interface Listener {
        void onThemeChanged(@NonNull CalculatorTheme theme);
    }

    private static volatile CalculatorTheme current = CalculatorThemes.defaultTheme();
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private ThemeManager() {
    }

    public static void init(@NonNull Context context) {
        current = CalculatorThemes.fromId(Preferences.getInstance(context).getSelectedThemeId());
    }

    @NonNull
    public static CalculatorTheme get() {
        return current;
    }

    public static void select(@NonNull Context context, @NonNull String themeId) {
        CalculatorTheme theme = CalculatorThemes.fromId(themeId);
        Preferences.getInstance(context).setSelectedThemeId(theme.id());
        current = theme;
        for (Listener listener : listeners) {
            listener.onThemeChanged(theme);
        }
    }

    public static void addListener(@NonNull Listener listener) {
        listeners.addIfAbsent(listener);
    }

    public static void removeListener(@Nullable Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
}
