package net.smartlogic.unitconverter.app;

import net.smartlogic.unitconverter.fragment.ConverterFragment;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Shriki on 4/8/2017.
 */

public class AppConst {

    public static final String TAG_CALC = "calculator";
    public static final String TAG_CONVERTER = "converter";
    public static final String TAG_EXPLORE = "explore";
    public static final String TAG_FAVORITES = "favorites";
    public static final String TAG_HISTORY = "history";

    /** @deprecated Use {@link #TAG_CONVERTER} with {@link #CONVERTER_TAB}. */
    @Deprecated
    public static final String TAG_UNIT = TAG_CONVERTER;
    /** @deprecated Use {@link #TAG_CONVERTER} with {@link #CONVERTER_TAB}. */
    @Deprecated
    public static final String TAG_CURRENCY = TAG_CONVERTER;

    public static String CURRENT_TAG = TAG_CALC;
    public static int CONVERTER_TAB = ConverterFragment.TAB_UNIT;

    public static DecimalFormat df0 = new DecimalFormat("####.##########");
    public static DecimalFormat df1 = new DecimalFormat("##,##,##,###.##########");
    public static DecimalFormat df2 = new DecimalFormat("###,###,###.##########");

    public static DecimalFormat activeDf = new DecimalFormat("####.##########");

    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d-MMM hh:mm aa", Locale.ENGLISH);
}
