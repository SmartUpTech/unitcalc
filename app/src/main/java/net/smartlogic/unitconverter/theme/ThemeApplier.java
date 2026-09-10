package net.smartlogic.unitconverter.theme;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import net.smartlogic.unitconverter.R;

public final class ThemeApplier {

    private static final int[] NUMBER_IDS = {
            R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four,
            R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine,
            R.id.dot, R.id.double_zero
    };
    private static final int[] FUNCTION_IDS = {
            R.id.power, R.id.sqrt, R.id.parenthesis_open, R.id.parenthesis_close,
            R.id.percent, R.id.sign_toggle
    };
    private static final int[] OPERATOR_IDS = {
            R.id.divide, R.id.multiply, R.id.minus, R.id.plus
    };
    private static final int[] ICON_IDS = {
            R.id.backspace, R.id.copy, R.id.reverse, R.id.btn_nav_menu,
            R.id.btn_clear_history
    };
    private static final int[] MAIN_TEXT_IDS = {
            R.id.result, R.id.input, R.id.output, R.id.inputSymbol, R.id.outputSymbol,
            R.id.txtRate, R.id.tv_choice_title, R.id.tv_action_title, R.id.tv_info_title,
            R.id.tv_history_result, R.id.textView, R.id.currency, R.id.currencyISO
    };
    private static final int[] SECONDARY_TEXT_IDS = {
            R.id.expression, R.id.tv_choice_summary, R.id.tv_action_summary,
            R.id.tv_choice_value, R.id.tv_info_value, R.id.tv_history_expression,
            R.id.tv_empty_history
    };

    private ThemeApplier() {
    }

    public static void apply(@Nullable View root) {
        if (root == null) {
            return;
        }
        applyRecursive(root, ThemeManager.get());
    }

    public static void applyNavigation(
            @Nullable BottomNavigationView bottomNav,
            @Nullable NavigationView drawer
    ) {
        CalculatorTheme theme = ThemeManager.get();
        ColorStateList selectedEqual = navigationTint(theme);
        if (bottomNav != null) {
            bottomNav.setBackgroundColor(theme.background());
            bottomNav.setItemIconTintList(selectedEqual);
            bottomNav.setItemTextColor(selectedEqual);
            bottomNav.setItemRippleColor(ColorStateList.valueOf(theme.functions() & 0x33FFFFFF | 0x33000000));
            bottomNav.setItemActiveIndicatorEnabled(false);
        }
        if (drawer != null) {
            drawer.setBackgroundColor(theme.background());
            drawer.setItemIconTintList(selectedEqual);
            drawer.setItemTextColor(selectedEqual);
            drawer.setItemBackground(null);
        }
    }

    @NonNull
    public static ColorStateList navigationTint(@NonNull CalculatorTheme theme) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{android.R.attr.state_selected},
                new int[]{}
        };
        int[] colors = new int[]{theme.equal(), theme.equal(), theme.functions()};
        return new ColorStateList(states, colors);
    }

    public static void applyTabLayout(@Nullable TabLayout tabLayout) {
        if (tabLayout == null) {
            return;
        }
        CalculatorTheme theme = ThemeManager.get();
        tabLayout.setBackgroundColor(theme.background());
        tabLayout.setTabTextColors(theme.functions(), theme.equal());
        tabLayout.setSelectedTabIndicatorColor(theme.equal());
    }

    @NonNull
    public static GradientDrawable roundedFill(@ColorInt int color, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    private static void applyRecursive(@NonNull View view, @NonNull CalculatorTheme theme) {
        int id = view.getId();
        if (id == R.id.cl
                || id == R.id.drawer_layout
                || id == R.id.settings
                || id == R.id.calculate_pane
                || id == R.id.keypad
                || id == R.id.frame_layout
                || id == R.id.history_root) {
            view.setBackgroundColor(theme.background());
        }

        if (view instanceof TabLayout) {
            applyTabLayout((TabLayout) view);
        }

        if (contains(NUMBER_IDS, id) && view instanceof TextView) {
            ((TextView) view).setTextColor(theme.mainText());
        } else if (contains(FUNCTION_IDS, id) && view instanceof TextView) {
            ((TextView) view).setTextColor(theme.functions());
        } else if (contains(OPERATOR_IDS, id) && view instanceof TextView) {
            ((TextView) view).setTextColor(theme.operatorContentColor());
        } else if (id == R.id.equal && view instanceof TextView) {
            ((TextView) view).setTextColor(theme.equal());
        } else if (contains(MAIN_TEXT_IDS, id) && view instanceof TextView) {
            ((TextView) view).setTextColor(theme.mainText());
        } else if (contains(SECONDARY_TEXT_IDS, id) && view instanceof TextView) {
            ((TextView) view).setTextColor(theme.functions());
        }

        if (contains(ICON_IDS, id) && view instanceof ImageView) {
            ImageViewCompat.setImageTintList((ImageView) view, ColorStateList.valueOf(theme.mainText()));
        }

        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecursive(group.getChildAt(i), theme);
            }
        }
    }

    private static boolean contains(@NonNull int[] ids, int id) {
        for (int candidate : ids) {
            if (candidate == id) {
                return true;
            }
        }
        return false;
    }
}
