package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatEditText;

import net.smartlogic.unitconverter.R;

/** Single-line amount editor; refits from its styled maximum on every measurement. */
public final class AutoSizeEditText extends AppCompatEditText {
    private final TextPaint measurementPaint = new TextPaint();
    private float maximumSp;
    private final float styledMaximumSp;
    private final int styledMinimumHeight;
    private boolean canonicalInput;
    private char displayedDecimal;

    public static void useCanonicalInput(android.widget.EditText field) {
        if (field instanceof AutoSizeEditText) {
            AutoSizeEditText editor = (AutoSizeEditText) field;
            editor.canonicalInput = true;
            editor.refreshDecimalDisplay();
        }
    }

    /** A length-preserving display transformation keeps selection and saved text canonical. */
    public void refreshDecimalDisplay() {
        if (!canonicalInput) return;
        final char decimal = net.smartlogic.unitconverter.helper.Preferences.getInstance(getContext())
                .getDecimalSeparator().charAt(0);
        if (decimal == displayedDecimal) return;
        displayedDecimal = decimal;
        int start = getSelectionStart(), end = getSelectionEnd();
        setTransformationMethod(new android.text.method.ReplacementTransformationMethod() {
            @Override protected char[] getOriginal() { return new char[]{'.'}; }
            @Override protected char[] getReplacement() { return new char[]{decimal}; }
        });
        if (start >= 0 && end >= 0) setSelection(Math.min(start, length()), Math.min(end, length()));
    }

    @Override public boolean onTextContextMenuItem(int id) {
        if (canonicalInput && (id == android.R.id.paste || id == android.R.id.pasteAsPlainText)) {
            var clipboard = (android.content.ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            var clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() != 1 || getText() == null) return true;
            try {
                String canonical = net.smartlogic.unitconverter.helper.Preferences.getInstance(getContext())
                        .getRegionalProfile().numbers.normalizeInput(clip.getItemAt(0).coerceToText(getContext()).toString());
                int start = Math.max(0, Math.min(getSelectionStart(), getSelectionEnd()));
                int end = Math.max(0, Math.max(getSelectionStart(), getSelectionEnd()));
                String proposed = getText().subSequence(0, start) + canonical + getText().subSequence(end, length());
                if (proposed.matches("[+-]?[0-9]*(?:\\.[0-9]*)?")) getText().replace(start, end, canonical);
            } catch (IllegalArgumentException invalid) {
                announceForAccessibility(getContext().getString(R.string.regional_invalid_number));
            }
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    public AutoSizeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        maximumSp = Build.VERSION.SDK_INT >= 34
                ? TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_SP, getTextSize(),
                        getResources().getDisplayMetrics())
                : getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        styledMaximumSp = maximumSp;
        styledMinimumHeight = getMinimumHeight();
    }

    /** Selects the shared compact numeric typography for constrained tool layouts. */
    public void setCompactValue(boolean compact) {
        float compactSp = getResources().getInteger(R.integer.converter_compact_value_max_sp);
        float maximum = compact ? Math.min(styledMaximumSp, compactSp) : styledMaximumSp;
        if (maximumSp != maximum) {
            maximumSp = maximum;
            forceLayout();
        }
    }

    @Override protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        requestLayout();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        // The row's geometry belongs to the maximum text role, not the current
        // fitted size. Recompute from the current typeface/scaling and padding so
        // resizing, deleting and clearing cannot pull the keypad upward.
        TextPaint probe = measurementPaint;
        probe.set(getPaint());
        probe.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, maximumSp,
                getResources().getDisplayMetrics()));
        android.graphics.Paint.FontMetricsInt metrics = probe.getFontMetricsInt();
        int lineHeight = getIncludeFontPadding() ? metrics.bottom - metrics.top
                : metrics.descent - metrics.ascent;
        setMinimumHeight(Math.max(styledMinimumHeight,
                lineHeight + getCompoundPaddingTop() + getCompoundPaddingBottom()));
        super.onMeasure(widthSpec, heightSpec);
        int width = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        if (width <= 0) return;
        float minimum = getResources().getInteger(R.integer.value_text_min_sp);
        float step = getResources().getInteger(R.integer.value_text_step_sp);
        CharSequence text = getText();
        if (getTransformationMethod() != null) {
            text = getTransformationMethod().getTransformation(text, this);
        }
        float size = maximumSp;
        while (true) {
            probe.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size,
                    getResources().getDisplayMetrics()));
            boolean fits = probe.measureText(text, 0, text.length()) <= width;
            if (size <= minimum || fits) break;
            size = Math.max(minimum, size - step);
        }
        if (Math.abs(getTextSize() - probe.getTextSize()) > 0.01f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, probe.getTextSize());
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}
