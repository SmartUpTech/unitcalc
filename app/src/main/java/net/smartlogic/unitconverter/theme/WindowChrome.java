package net.smartlogic.unitconverter.theme;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import net.smartlogic.unitconverter.R;

/**
 * Applies status bar, system navigation bar, and action bar from the active
 * {@link CalculatorTheme} using Window APIs that behave the same on AOSP and
 * OEM wrappers (ColorOS, Funtouch, etc.). Does not use DayNight or force-dark.
 */
public final class WindowChrome {

    private WindowChrome() {
    }

    public static void apply(@NonNull Activity activity) {
        CalculatorTheme theme = ThemeManager.get();
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);

        int chrome = theme.background;
        window.setStatusBarColor(chrome);
        window.setNavigationBarColor(chrome);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat insets =
                WindowCompat.getInsetsController(window, window.getDecorView());
        boolean lightBarIcons = theme.isLightBackground();
        insets.setAppearanceLightStatusBars(lightBarIcons);
        insets.setAppearanceLightNavigationBars(lightBarIcons);

        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setBackgroundDrawable(new ColorDrawable(chrome));
                View custom = actionBar.getCustomView();
                if (custom != null) {
                    applyActionBarBrand(custom, theme);
                }
            }
        }
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            content.setBackgroundColor(chrome);
        }
    }

    private static void applyActionBarBrand(@NonNull View custom, @NonNull CalculatorTheme theme) {
        custom.setBackgroundColor(theme.background);
        ImageView menu = custom.findViewById(R.id.btn_nav_menu);
        if (menu != null && menu.getDrawable() != null) {
            DrawableCompat.setTint(DrawableCompat.wrap(menu.getDrawable().mutate()), theme.mainText);
        }
        TextView brand = custom.findViewById(R.id.tv_app_bar_brand);
        if (brand != null) {
            brand.setTextColor(theme.mainText);
        }
        TextView product = custom.findViewById(R.id.tv_app_bar_product);
        if (product != null) {
            product.setTextColor(theme.functions);
        }
    }
}
