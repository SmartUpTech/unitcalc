package net.smartlogic.unitconverter.theme;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import net.smartlogic.unitconverter.R;

/** Shared typography boundary for XML, dynamically added rows, dialogs and Canvas text. */
public final class AppearanceTypography {
    private AppearanceTypography() {}
    public static Typeface typeface(Context context) {
        return ResourcesCompat.getFont(context, Appearance.font(context).resource);
    }
    public static void apply(View view) {
        Appearance.Font font = Appearance.font(view.getContext());
        apply(view, font, ResourcesCompat.getFont(view.getContext(), font.resource));
    }
    private static void apply(View view, Appearance.Font font, Typeface selected) {
        if (view instanceof com.google.android.material.navigation.NavigationView navigation) {
            applyMenu(navigation.getMenu(), selected);
        }
        if (view instanceof TextView text) {
            Typeface expected = text.isSelected() && isWorkspaceTabLabel(text)
                    ? Typeface.create(selected, Typeface.BOLD) : selected;
            // Material widgets can restore their text appearance after a font was applied.
            if (text.getTag(R.id.appearance_font_applied) != font
                    || !expected.equals(text.getTypeface())) {
                text.setTypeface(expected);
                text.setTag(R.id.appearance_font_applied, font);
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) apply(group.getChildAt(i), font, selected);
        }
    }
    // Drawer rows are recycled: bind typography to titles even before a row is attached.
    public static void applyMenu(Context context, Menu menu) {
        applyMenu(menu, typeface(context));
    }
    private static void applyMenu(Menu menu, Typeface selected) {
        for (int i = 0; i < menu.size(); i++) {
            var item = menu.getItem(i);
            CharSequence title = item.getTitle();
            if (title != null && title.length() > 0) {
                MenuFontSpan[] old = title instanceof Spanned spanned
                        ? spanned.getSpans(0, title.length(), MenuFontSpan.class) : new MenuFontSpan[0];
                if (old.length != 1 || !selected.equals(old[0].typeface)) {
                    SpannableString styled = new SpannableString(title);
                    for (MenuFontSpan span : old) styled.removeSpan(span);
                    styled.setSpan(new MenuFontSpan(selected), 0, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    item.setTitle(styled);
                }
            }
            if (item.hasSubMenu()) applyMenu(item.getSubMenu(), selected);
        }
    }
    private static final class MenuFontSpan extends MetricAffectingSpan {
        private final Typeface typeface;
        private MenuFontSpan(Typeface typeface) { this.typeface = typeface; }
        @Override public void updateDrawState(TextPaint paint) { updateMeasureState(paint); }
        @Override public void updateMeasureState(TextPaint paint) {
            paint.setTypeface(typeface);
            paint.setFakeBoldText(false);
            paint.setTextSkewX(0);
        }
    }
    private static boolean isWorkspaceTabLabel(TextView text) {
        for (ViewParent parent = text.getParent(); parent instanceof View; parent = parent.getParent()) {
            if (((View) parent).getId() == R.id.workspace_tabs) return true;
        }
        return false;
    }
    public static void bind(View root) {
        if (root.getTag(R.id.appearance_typography_bound) == null) {
            root.setTag(R.id.appearance_typography_bound, Boolean.TRUE);
            root.getViewTreeObserver().addOnGlobalLayoutListener(() -> apply(root));
        }
        apply(root);
    }
    public static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {
                View decor = activity.getWindow().getDecorView();
                bind(decor);
            }
            @Override public void onActivityResumed(Activity a) { apply(a.getWindow().getDecorView()); }
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }
}
