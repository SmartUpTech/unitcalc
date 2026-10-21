package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatSpinner;

/** Retain Spinner selection/rendering while routing every activation to the shared dialog. */
public final class ConverterPickerSpinner extends AppCompatSpinner {
    private Runnable pickerClick;
    public ConverterPickerSpinner(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setPickerClick(Runnable click) { pickerClick = click; }
    @Override public boolean performClick() {
        if (pickerClick != null) pickerClick.run();
        return true;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        // Bypass AppCompat's dropdown forwarding gesture as well as its click path.
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getX() < 0 || event.getX() >= getWidth()
                        || event.getY() < 0 || event.getY() >= getHeight()) setPressed(false);
                return true;
            case MotionEvent.ACTION_UP:
                boolean activate = isPressed();
                setPressed(false);
                if (activate) performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                return true;
            default:
                return true;
        }
    }
}
