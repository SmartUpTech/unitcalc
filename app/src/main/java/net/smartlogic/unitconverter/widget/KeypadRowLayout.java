package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * A horizontal LinearLayout for keypad rows that automatically supplies default layout parameters
 * (width: 0dp, height: match_parent, weight: 1) for child views (such as keys sharing the common
 * CalculatorKey style) that omit layout dimensions in XML.
 */
public final class KeypadRowLayout extends LinearLayout {
    public KeypadRowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        try {
            return super.generateLayoutParams(attrs);
        } catch (UnsupportedOperationException e) {
            return new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        }
    }
}
