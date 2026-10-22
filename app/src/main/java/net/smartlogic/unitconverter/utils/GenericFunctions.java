package net.smartlogic.unitconverter.utils;

import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import net.smartlogic.unitconverter.R;

public class GenericFunctions {

    public static void showToast(View v, int message) {
        Snackbar sb = Snackbar.make(v, message, Snackbar.LENGTH_LONG);
        net.smartlogic.unitconverter.theme.CalculatorTheme theme = net.smartlogic.unitconverter.theme.ThemeManager.get();
        sb.setBackgroundTint(theme.background());
        sb.setTextColor(theme.mainText());
        sb.setActionTextColor(theme.equal());
        sb.show();
    }

    public static void adjustTextSize(final TextView textView, final float maxSp) {
        if (textView == null) return;

        // If width is not yet measured, post to wait for layout
        if (textView.getWidth() <= 0) {
            textView.post(() -> adjustTextSize(textView, maxSp));
            return;
        }

        CharSequence text = textView.getText();
        if (text == null || text.length() == 0) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxSp);
            textView.setHorizontallyScrolling(false);
            textView.scrollTo(0, 0);
            return;
        }

        float availableWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
        if (availableWidth <= 0) return;

        TextPaint paint = new TextPaint(textView.getPaint());
        android.util.DisplayMetrics metrics = textView.getResources().getDisplayMetrics();
        float targetSizeSp = maxSp;

        // Reduction loop: start from max and go down until it fits or hits 8sp
        while (targetSizeSp > 8) {
            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, targetSizeSp, metrics));
            if (paint.measureText(text, 0, text.length()) > availableWidth) {
                targetSizeSp -= 2;
            } else {
                break;
            }
        }
        targetSizeSp = Math.max(8, targetSizeSp);

        if (Math.abs(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, targetSizeSp, metrics) - textView.getTextSize()) > 0.1) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, targetSizeSp);
        }

        paint.setTextSize(textView.getTextSize());
        boolean overflow = paint.measureText(text, 0, text.length()) > availableWidth;
        textView.setHorizontallyScrolling(overflow);
        if (overflow) {
            textView.post(() -> {
                int scrollX = (int) (paint.measureText(text, 0, text.length()) - availableWidth);
                textView.scrollTo(Math.max(0, scrollX), 0);
            });
        } else {
            textView.scrollTo(0, 0);
        }
    }

    public static void resetTextSize(final TextView textView, final float maxSp) {
        if (textView == null) return;
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxSp);
        adjustTextSize(textView, maxSp);
    }
}
