package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.smartlogic.unitconverter.R;

/** Give the current amount enough width before abbreviating a picker label. */
public final class ConverterValueRow extends LinearLayout {
    public ConverterValueRow(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        ViewGroup owner = (ViewGroup) getParent();
        while (owner != null && owner.getId() != R.id.calculation_controls) {
            owner = owner.getParent() instanceof ViewGroup ? (ViewGroup) owner.getParent() : null;
        }
        if (owner != null) {
            int available = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
            LayoutParams amount = (LayoutParams) getChildAt(0).getLayoutParams();
            LayoutParams picker = (LayoutParams) getChildAt(1).getLayoutParams();
            int pickerMinimum = dimension(R.dimen.converter_touch_target)
                    + dimension(R.dimen.converter_chevron_size) + 2 * dimension(R.dimen.converter_spacing);
            TextView iso = owner.findViewById(R.id.fromCurrencyISO);
            if (iso != null) {
                TextView toIso = owner.findViewById(R.id.toCurrencyISO);
                pickerMinimum = dimension(R.dimen.converter_flag_height)
                        + (int) Math.ceil(Math.max(textWidth(iso), textWidth(toIso)))
                        + dimension(R.dimen.converter_chevron_size) + 4 * dimension(R.dimen.converter_spacing);
            }
            TextView input = owner.findViewById(R.id.input);
            TextView output = owner.findViewById(R.id.output);
            int content = available - amount.leftMargin - amount.rightMargin;
            int preferred = (int) Math.ceil(Math.max(textWidth(input), textWidth(output)))
                    + dimension(R.dimen.converter_small_spacing);
            amount.width = Math.max(0, Math.min(content - pickerMinimum,
                    Math.max((int) (content / 2.25f), preferred)));
            picker.width = Math.max(0, content - amount.width);
            picker.weight = 0;
            amount.weight = 0;
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    private float textWidth(TextView text) {
        return text.getPaint().measureText(text.getText().toString())
                + text.getCompoundPaddingLeft() + text.getCompoundPaddingRight();
    }

    private int dimension(int id) { return getResources().getDimensionPixelSize(id); }
}
