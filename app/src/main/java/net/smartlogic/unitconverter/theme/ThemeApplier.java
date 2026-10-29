package net.smartlogic.unitconverter.theme;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
            R.id.percent, R.id.sign_toggle, R.id.ac
    };
    private static final int[] OPERATOR_IDS = {
            R.id.divide, R.id.multiply, R.id.minus, R.id.plus
    };
    private static final int[] ICON_IDS = {
            R.id.backspace, R.id.copy, R.id.reverse, R.id.reverse_destinations, R.id.btn_nav_menu,
            R.id.refresh, R.id.catalog_icon,
            R.id.btn_favorite, R.id.fromChevron,
            R.id.toChevron, R.id.additionalToChevron
    };
    private static final int[] MAIN_TEXT_IDS = {
            R.id.result, R.id.input, R.id.output, R.id.additionalOutput,
            R.id.additionalToCurrency, R.id.additionalToCurrencyISO,
            R.id.tv_choice_title, R.id.tv_action_title, R.id.tv_info_title,
            R.id.tv_history_result, R.id.textView,
            R.id.fromCurrency, R.id.toCurrency,
            R.id.catalog_title, R.id.tv_history_section,
            R.id.fromCurrencyISO, R.id.toCurrencyISO, R.id.tool_space_message,
            R.id.tv_empty_history, R.id.history_start
    };
    private static final int[] SECONDARY_TEXT_IDS = {
            R.id.expression, R.id.tv_choice_summary, R.id.tv_action_summary,
            R.id.tv_choice_value, R.id.tv_info_value, R.id.tv_history_expression,
            R.id.txtLastUpdateTime, R.id.tv_history_time, R.id.tv_history_calculator,
            R.id.converter_from_label, R.id.converter_to_label,
            R.id.unit_rate, R.id.txtRate, R.id.inputSymbol, R.id.outputSymbol, R.id.additionalOutputSymbol,
            R.id.history_reuse_hint, R.id.history_empty_description, R.id.history_legacy_hint
    };
    private static final int[] CLEAR_BACKGROUND_IDS = {
            R.id.converter_card, R.id.reverse_container
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
            bottomNav.setItemRippleColor(ColorStateList.valueOf(theme.secondaryContentColor() & 0x33FFFFFF | 0x33000000));
            bottomNav.setItemActiveIndicatorEnabled(false);
        }
        if (drawer != null) {
            drawer.setBackgroundColor(theme.background());
            drawer.setItemIconTintList(selectedEqual);
            drawer.setItemTextColor(selectedEqual);
            drawer.setItemBackground(null);
            // NavigationView does not apply itemTextColor to section subheaders.
            for (int i = 0; i < drawer.getMenu().size(); i++) {
                var item = drawer.getMenu().getItem(i);
                if (!item.hasSubMenu()) continue;
                var title = new SpannableString(item.getTitle().toString());
                title.setSpan(new ForegroundColorSpan(theme.secondaryContentColor()),
                        0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                item.setTitle(title);
            }
        }
    }

    @NonNull
    public static ColorStateList navigationTint(@NonNull CalculatorTheme theme) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{android.R.attr.state_selected},
                new int[]{}
        };
        int[] colors = new int[]{theme.accentContentColor(), theme.accentContentColor(), theme.secondaryContentColor()};
        return new ColorStateList(states, colors);
    }

    public static void applyTabLayout(@Nullable TabLayout tabLayout) {
        if (tabLayout == null) {
            return;
        }
        CalculatorTheme theme = ThemeManager.get();
        tabLayout.setBackgroundColor(theme.background());
        tabLayout.setTabTextColors(theme.secondaryContentColor(), theme.accentContentColor());
        tabLayout.setSelectedTabIndicatorColor(theme.accentContentColor());
    }

    @NonNull
    public static GradientDrawable roundedFill(@ColorInt int color, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    /** Shared styling for user-reviewed action dialogs. */
    public static void applyDialog(@NonNull androidx.appcompat.app.AlertDialog dialog) {
        CalculatorTheme theme = ThemeManager.get();
        TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        TextView message = dialog.findViewById(android.R.id.message);
        if (title != null) title.setTextColor(theme.primaryContentColor());
        if (message != null) message.setTextColor(theme.primaryContentColor());
        for (int button : new int[]{-1, -2, -3}) {
            if (dialog.getButton(button) != null) dialog.getButton(button).setTextColor(theme.accentContentColor());
        }
    }

    private static void applyRecursive(@NonNull View view, @NonNull CalculatorTheme theme) {
        int id = view.getId();
        if (id == R.id.cl
                || id == R.id.drawer_layout
                || id == R.id.settings
                || id == R.id.calculate_pane
                || id == R.id.age_calculate_pane
                || id == R.id.keypad
                || id == R.id.frame_layout
                || id == R.id.history_root
                || id == R.id.main_content
                || id == R.id.ll_main
                || id == R.id.convert_pane
                || id == R.id.converter_container
                || id == R.id.explore_root
                || id == R.id.favorites_root) {
            view.setBackgroundColor(theme.background());
        }

        if (view instanceof TabLayout) {
            applyTabLayout((TabLayout) view);
        }
        if (view instanceof TextView text) {
            if ("content_primary".equals(view.getTag())) text.setTextColor(theme.primaryContentColor());
            if ("content_secondary".equals(view.getTag())) text.setTextColor(theme.secondaryContentColor());
            if ("content_accent".equals(view.getTag())) text.setTextColor(theme.accentContentColor());
        }
        if ("theme_outlined_surface".equals(view.getTag())) {
            int outline = view.getResources().getDimensionPixelSize(R.dimen.theme_outline);
            GradientDrawable surface = roundedFill(theme.background(), view.getResources().getDimension(R.dimen.converter_card_padding));
            surface.setStroke(outline, withAlpha(theme.secondaryContentColor(), 0x66));
            view.setBackground(new android.graphics.drawable.InsetDrawable(surface, outline, outline, outline, outline));
        }
        if ("theme_divider".equals(view.getTag())) view.setBackgroundColor(withAlpha(theme.secondaryContentColor(), 0x44));
        if ("content_accent".equals(view.getTag()) && view instanceof ImageView icon) {
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(theme.accentContentColor()));
        }
        if ("theme_accent_badge".equals(view.getTag()) && view instanceof ImageView icon) {
            icon.setBackground(roundedFill(theme.accentContentColor(), view.getResources().getDimension(R.dimen.graphy_touch_target)));
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(theme.onAccentColor()));
        }
        if (("theme_outline_action".equals(view.getTag()) || "theme_outline_field".equals(view.getTag()) || "theme_text_action".equals(view.getTag())
                || "theme_filled_action".equals(view.getTag())) && view instanceof com.google.android.material.button.MaterialButton button) {
            boolean filled = "theme_filled_action".equals(view.getTag());
            int ink = filled ? theme.onAccentColor() : "theme_outline_field".equals(view.getTag()) ? theme.primaryContentColor() : theme.accentContentColor();
            int disabledSurface = androidx.core.graphics.ColorUtils.compositeColors(withAlpha(theme.secondaryContentColor(), 0x18), theme.background());
            button.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
                    new int[]{CalculatorTheme.readableColor(theme.secondaryContentColor(), filled ? disabledSurface : theme.background()), ink}));
            button.setIconTint(ColorStateList.valueOf(ink));
            button.setBackgroundTintList(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
                    new int[]{filled ? disabledSurface : theme.background(), filled ? theme.accentContentColor() : theme.background()}));
            button.setStrokeColor(ColorStateList.valueOf(theme.secondaryContentColor()));
            button.setStrokeWidth(("theme_outline_action".equals(view.getTag()) || "theme_outline_field".equals(view.getTag())) ? view.getResources().getDimensionPixelSize(R.dimen.theme_outline) : 0);
            button.setRippleColor(ColorStateList.valueOf(withAlpha(ink, 0x22)));
        }
        if (id == R.id.converter_divider) {
            view.setBackgroundColor(withAlpha(theme.secondaryContentColor(), 0x33));
        }
        if (id == R.id.history_row_divider) view.setBackgroundColor(withAlpha(theme.secondaryContentColor(), 0x26));
        if ("history_secondary".equals(view.getTag()) && view instanceof TextView tv) tv.setTextColor(theme.secondaryContentColor());
        if (id == R.id.history_start && view instanceof com.google.android.material.button.MaterialButton button) {
            button.setRippleColor(ColorStateList.valueOf(withAlpha(theme.primaryContentColor(), 0x22)));
        }
        if (id == R.id.history_loading && view instanceof android.widget.ProgressBar progress) {
            progress.setIndeterminateTintList(ColorStateList.valueOf(theme.secondaryContentColor()));
        }
        if ((id == R.id.btn_unit_mode || id == R.id.btn_currency_mode)
                && view instanceof com.google.android.material.chip.Chip chip) {
            int[][] states = {{android.R.attr.state_checked}, {}};
            chip.setTextColor(new ColorStateList(states,
                    new int[]{theme.primaryContentColor(), theme.secondaryContentColor()}));
            chip.setChipBackgroundColor(new ColorStateList(states,
                    new int[]{androidx.core.graphics.ColorUtils.compositeColors(
                            withAlpha(theme.secondaryContentColor(), 0x26), theme.background()), theme.background()}));
            chip.setChipStrokeColor(ColorStateList.valueOf(withAlpha(theme.secondaryContentColor(), 0x88)));
        }
        if ((id == R.id.btn_unit_mode || id == R.id.btn_currency_mode)
                && view instanceof com.google.android.material.button.MaterialButton button) {
            int[][] states = {{android.R.attr.state_checked}, {}};
            int selectedText = theme.onAccentColor();
            button.setTextColor(new ColorStateList(states,
                    new int[]{selectedText, theme.primaryContentColor()}));
            button.setBackgroundTintList(new ColorStateList(states,
                    new int[]{theme.accentContentColor(), Color.TRANSPARENT}));
            button.setStrokeWidth(0);
            button.setRippleColor(new ColorStateList(states, new int[]{
                    withAlpha(selectedText, 0x22), withAlpha(theme.primaryContentColor(), 0x22)}));
        }
        if (id == R.id.converter_mode_toggle) {
            GradientDrawable track = new GradientDrawable();
            track.setColor(androidx.core.graphics.ColorUtils.compositeColors(
                    withAlpha(theme.secondaryContentColor(), 0x18), theme.background()));
            track.setCornerRadius(view.getResources().getDimension(R.dimen.converter_mode_height));
            int inset = view.getResources().getDimensionPixelSize(R.dimen.converter_mode_vertical_inset);
            view.setBackground(new android.graphics.drawable.InsetDrawable(track, 0, inset, 0, inset));
            view.setPadding(0, 0, 0, 0);
        }
        if ("converter_selector".equals(view.getTag()) || "converter_amount".equals(view.getTag())) {
            int left = view.getPaddingLeft();
            int right = view.getPaddingRight();
            if ("converter_selector".equals(view.getTag())) {
                left = right = view.getResources().getDimensionPixelSize(R.dimen.converter_field_content_inset)
                        + view.getResources().getDimensionPixelSize(R.dimen.converter_selector_pixel_adjustment);
            }
            GradientDrawable outline = roundedFill(Color.TRANSPARENT,
                    view.getResources().getDimension(R.dimen.converter_selector_radius));
            outline.setStroke(view.getResources().getDimensionPixelSize(R.dimen.converter_divider_height),
                    theme.secondaryContentColor());
            view.setBackground(outline);
            view.setPadding(left, 0, right, 0);
        }

        boolean isKey = contains(NUMBER_IDS, id) || contains(FUNCTION_IDS, id) || contains(OPERATOR_IDS, id) || contains(ICON_IDS, id) || id == R.id.equal;

        if (isKey || contains(CLEAR_BACKGROUND_IDS, id)) {
            if (view instanceof com.google.android.material.button.MaterialButton mb
                    && mb.getParent() instanceof com.google.android.material.button.MaterialButtonToggleGroup) {
                mb.setBackgroundTintList(null);
            } else {
                view.setBackground(null);
            }
        }

        if (contains(NUMBER_IDS, id) && view instanceof TextView tv) {
            tv.setTextColor(theme.primaryContentColor());
        } else if (contains(FUNCTION_IDS, id) && view instanceof TextView tv) {
            tv.setTextColor(theme.secondaryContentColor());
        } else if (contains(OPERATOR_IDS, id) && view instanceof TextView tv) {
            tv.setTextColor(theme.operatorContentColor());
        } else if (id == R.id.equal && view instanceof TextView tv) {
            tv.setTextColor(theme.accentContentColor());
        } else if (contains(MAIN_TEXT_IDS, id) && view instanceof TextView tv) {
            tv.setTextColor(theme.primaryContentColor());
            tv.setHintTextColor(theme.secondaryContentColor());
        } else if (contains(SECONDARY_TEXT_IDS, id) && view instanceof TextView tv) {
            tv.setTextColor(theme.secondaryContentColor());
        }

        if ((contains(ICON_IDS, id) || id == R.id.equal) && view instanceof ImageView iv) {
            ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(id == R.id.equal
                    ? theme.accentContentColor() : theme.primaryContentColor()));
        }
        if (id == R.id.history_item_more && view instanceof ImageView iv) {
            ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(theme.secondaryContentColor()));
        }
        if (id == R.id.converter_category_tab_icon && view instanceof ImageView iv) {
            ImageViewCompat.setImageTintList(iv, navigationTint(theme));
        }
        if (id == R.id.converter_category_tab_label && view instanceof TextView tv) {
            tv.setTextColor(navigationTint(theme));
        }

        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecursive(group.getChildAt(i), theme);
            }
        }
    }

    private static int withAlpha(@ColorInt int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
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
