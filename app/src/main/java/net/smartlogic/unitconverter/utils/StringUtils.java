package net.smartlogic.unitconverter.utils;

public class StringUtils {

    public static Boolean isNotBlank(String str) {
        return !str.isEmpty();
    }

    public static boolean isEmpty(String s) {
        return (s == null || s.isEmpty());
    }
}
