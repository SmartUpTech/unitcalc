package net.smartlogic.unitconverter.utils;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import net.smartlogic.unitconverter.R;

public class GenericFunctions {

    public static void showToast(View v, int message) {
        Snackbar sb = Snackbar.make(v, message, Snackbar.LENGTH_LONG);
        sb.getView().setBackgroundResource(R.color.colorNewGrey);
        sb.show();
    }

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
