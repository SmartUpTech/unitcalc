package net.smartlogic.unitconverter.utils;

import java.util.ArrayList;

public class StringUtils {

    public static Boolean isNotBlank(String str) {
        return !str.equals("");
    }

    public static boolean isEmpty(String s) {
        return (s == null || s.isEmpty());
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static String ArrayListToCommaSeparatedString(ArrayList<String> array) {
        String SEPARATOR = ",";
        try {

            StringBuilder csvBuilder = new StringBuilder();
            for (String str : array) {
                csvBuilder.append(str);
                csvBuilder.append(SEPARATOR);
            }

            String csv = csvBuilder.toString();
            csv = csv.substring(0, csv.length() - SEPARATOR.length());

            return csv;
        }
        catch (Exception e) {
            return "";
        }
    }
}
