package net.smartlogic.unitconverter.app;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Shriki on 4/8/2017.
 */

public class AppConst {

    public static final String TAG_UNIT     = "converter";
    public static final String TAG_CURRENCY = "currency_converter";
    public static String CURRENT_TAG        = TAG_UNIT;


    public static DecimalFormat df0 = new DecimalFormat("####.##########");
    public static DecimalFormat df1 = new DecimalFormat("##,##,##,###.##########");
    public static DecimalFormat df2 = new DecimalFormat("###,###,###.##########");

    public static DecimalFormat activeDf = new DecimalFormat("####.##########");

    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d-MMM hh:mm aa", Locale.ENGLISH);
}