package net.smartlogic.unitconverter.utils;


import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtils {

        //private static DecimalFormat df = new DecimalFormat("##,##,##,###");

        public static Integer parseInt(String str) {
            return StringUtils.isNotBlank(str) ? Integer.parseInt(str.replace(",","")) : 0;
        }

        public static Float parseFloat(String str) {
            return StringUtils.isNotBlank(str) ? Float.parseFloat(str.replace(",","")) : 0;
        }

        public static Double parseDouble(String str) {
            return StringUtils.isNotBlank(str) & !str.equals("-") ? Double.parseDouble(str.replace(",","").replace("%","")) : 0;
        }

        public static Long parseLong(String str) {
            return StringUtils.isNotBlank(str) ? Long.parseLong(str.replace(",","")) : 0;
        }





}
