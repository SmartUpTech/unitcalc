package net.smartlogic.unitconverter.theme;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import net.smartlogic.unitconverter.R;

/** Native preview component using the production keypad role and appearance renderer. */
public final class KeypadPreviewView extends LinearLayout {
    public KeypadPreviewView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }
    public void render(CalculatorTheme theme, Appearance.Style style, Appearance.Shape shape,
            Appearance.Font font) {
        removeAllViews();
        setBackgroundColor(theme.background());
        int[] ids = {R.id.seven, R.id.ac, R.id.divide, R.id.backspace, R.id.equal};
        for (int id : ids) {
            View key;
            if (id == R.id.backspace) {
                ImageView icon = new ImageView(getContext());
                icon.setImageResource(R.drawable.backspace);
                icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                int inset = getResources().getDimensionPixelSize(R.dimen.keypad_icon_padding);
                icon.setPadding(inset, inset, inset, inset);
                key = icon;
            } else {
                TextView text = new TextView(getContext());
                text.setGravity(Gravity.CENTER);
                text.setIncludeFontPadding(false);
                text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.appearance_preview_text));
                text.setTypeface(ResourcesCompat.getFont(getContext(), font.resource));
                text.setText(id == R.id.seven ? R.string.seven
                        : id == R.id.ac ? R.string.converter_clear_key
                        : id == R.id.divide ? R.string.divide : R.string.equal);
                key = text;
            }
            addView(key, new LayoutParams(0, -1, 1));
            ThemeApplier.applyKey(key, theme, style, shape, ThemeApplier.keyRole(id));
        }
    }
}
