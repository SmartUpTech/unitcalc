package net.smartlogic.unitconverter.utils;


import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtils {

        //private static DecimalFormat df = new DecimalFormat("##,##,##,###");

        public static Integer parseInt(String str) {
            if (!StringUtils.isNotBlank(str)) return 0;
            String clean = str.replaceAll("[^0-9\\-]", "");
            return clean.isEmpty() || clean.equals("-") ? 0 : Integer.parseInt(clean);
        }

        public static Float parseFloat(String str) {
            if (!StringUtils.isNotBlank(str)) return 0f;
            String clean = str.replaceAll("[^0-9.\\-]", "");
            return clean.isEmpty() || clean.equals("-") ? 0f : Float.parseFloat(clean);
        }

        public static Double parseDouble(String str) {
            if (!StringUtils.isNotBlank(str) || str.equals("-")) return 0.0;
            String clean = str.replaceAll("[^0-9.\\-]", "");
            return clean.isEmpty() || clean.equals("-") ? 0.0 : Double.parseDouble(clean);
        }

        public static Long parseLong(String str) {
            if (!StringUtils.isNotBlank(str)) return 0L;
            String clean = str.replaceAll("[^0-9\\-]", "");
            return clean.isEmpty() || clean.equals("-") ? 0L : Long.parseLong(clean);
        }





}
