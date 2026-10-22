package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import net.smartlogic.unitconverter.R;

/** A joined mode track whose selected segment retains a complete pill shape. */
public final class ConverterModeToggleGroup extends MaterialButtonToggleGroup {
    public ConverterModeToggleGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        for (int i = 0; i < getChildCount(); i++) {
            MaterialButton button = (MaterialButton) getChildAt(i);
            // The group rebuilds joined corners on each measurement. Reapply the
            // actual half-height radius rather than MaterialButton's cached radius.
            button.setShapeAppearanceModel(button.getShapeAppearanceModel().toBuilder()
                    .setAllCornerSizes(Math.min(button.getWidth(), button.getHeight()
                            - 2 * getResources().getDimensionPixelSize(R.dimen.converter_mode_vertical_inset)) / 2f).build());
        }
    }
}
