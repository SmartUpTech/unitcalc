package net.smartlogic.unitconverter.utils;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import net.smartlogic.unitconverter.R;

public class GenericFunctions {

    public static void showToast(View v, int message) {
        Snackbar sb = Snackbar.make(v, message, Snackbar.LENGTH_LONG);
        sb.getView().setBackgroundResource(R.color.colorNewGrey);
        sb.show();
    }
}
