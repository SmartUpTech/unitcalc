package net.smartlogic.unitconverter.theme;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
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
        if (view instanceof TextView text) {
            // Material widgets can restore their text appearance after a font was applied.
            if (text.getTag(R.id.appearance_font_applied) != font
                    || !selected.equals(text.getTypeface())) {
                text.setTypeface(selected, Typeface.NORMAL);
                text.setTag(R.id.appearance_font_applied, font);
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) apply(group.getChildAt(i), font, selected);
        }
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
