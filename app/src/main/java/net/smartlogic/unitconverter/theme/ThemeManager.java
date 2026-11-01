package net.smartlogic.unitconverter.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.smartlogic.unitconverter.helper.Preferences;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

public final class ThemeManager {

    public interface Listener {
        void onThemeChanged(@NonNull CalculatorTheme theme);
    }

    private static volatile CalculatorTheme current = CalculatorThemes.defaultTheme();
    private static volatile BooleanSupplier proAccess = () -> false;
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> accessListeners = new CopyOnWriteArrayList<>();

    private ThemeManager() {
    }

    public static void init(@NonNull Context context) {
        Preferences preferences = Preferences.getInstance(context);
        current = CalculatorThemes.fromId(preferences.reconcileThemeAccess(
                System.currentTimeMillis(), hasProAccess(), true));
    }

    /** Install the paid entitlement source before init; never supply temporary ad-free status. */
    public static void setProAccessProvider(@NonNull BooleanSupplier provider) { proAccess = provider; }

    public static boolean hasProAccess() { return proAccess.getAsBoolean(); }

    /** Also call when the paid entitlement changes while the app is visible. */
    public static void refreshAccess(@NonNull Context context) {
        String id = Preferences.getInstance(context).reconcileThemeAccess(System.currentTimeMillis(),
                hasProAccess(), !ThemeRewardCoordinator.isBusy());
        publish(CalculatorThemes.fromId(id));
        for (Runnable listener : accessListeners) listener.run();
        ThemeAccessLifecycle.dispatchPendingNotice(context);
    }

    @NonNull
    public static CalculatorTheme get() {
        return current;
    }

    public static void select(@NonNull Context context, @NonNull String themeId) {
        if (!Preferences.getInstance(context).setSelectedThemeId(themeId)) {
            refreshAccess(context);
            return;
        }
        publish(CalculatorThemes.fromId(themeId));
        net.smartlogic.unitconverter.helper.UsageAnalytics.event("theme_selected", "theme_id", themeId);
    }

    private static void publish(CalculatorTheme theme) {
        if (theme.id().equals(current.id())) return;
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

    public static void addAccessListener(Runnable listener) { accessListeners.addIfAbsent(listener); }
    public static void removeAccessListener(Runnable listener) { accessListeners.remove(listener); }
}
