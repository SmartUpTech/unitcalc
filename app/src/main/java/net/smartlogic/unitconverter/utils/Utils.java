package net.smartlogic.unitconverter.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.TypedValue;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.MainActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class Utils {

    private Context context;

    public Utils() {
        // Required empty public constructor
    }

    public Utils(Context context) {
        this.context = context;
    }

    public void replaceFragment(MainActivity mainActivity, Fragment fragment) {
        FragmentTransaction transaction = mainActivity.getSupportFragmentManager().beginTransaction();
        //transaction.addToBackStack("MenuFragment");
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }

    public void addFragment(MainActivity mainActivity, Fragment fragment) {
        FragmentTransaction transaction = mainActivity.getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public boolean isNetworkAvailable(Context context) {

        //Log.d("SHRIKI", "Inside isNetworkAvailable");
        int[] networkTypes = {ConnectivityManager.TYPE_MOBILE,
                ConnectivityManager.TYPE_WIFI};
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            for (int networkType : networkTypes) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null &&
                        activeNetworkInfo.getType() == networkType)
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }



}
