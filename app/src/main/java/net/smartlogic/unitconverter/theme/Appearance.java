package net.smartlogic.unitconverter.theme;

import android.content.Context;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;

/** Stable persisted identifiers; theme selection and access remain owned by ThemeManager. */
public final class Appearance {
    public enum Style {
        FLAT(R.string.button_style_flat), SOFT_FILL(R.string.button_style_soft_fill),
        OUTLINE(R.string.button_style_outline), TONAL(R.string.button_style_tonal),
        RAISED(R.string.button_style_raised), INSET(R.string.button_style_inset),
        BEVEL(R.string.button_style_bevel), SATIN(R.string.button_style_satin);
        public final int label;
        Style(int label) { this.label = label; }
    }
    public enum Shape {
        SQUARE(R.string.button_shape_square), ROUNDED(R.string.button_shape_rounded),
        SQUIRCLE(R.string.button_shape_squircle), CIRCLE(R.string.button_shape_circle);
        public final int label;
        Shape(int label) { this.label = label; }
    }
    public enum Font {
        OSWALD("Oswald", R.font.oswald_regular), ROBOTO("Roboto", R.font.roboto_regular),
        OPEN_SANS("Open Sans", R.font.open_sans_regular), LATO("Lato", R.font.lato_regular),
        ROBOTO_MONO("Roboto Mono", R.font.roboto_mono_regular);
        public final String label;
        public final int resource;
        Font(String label, int resource) { this.label = label; this.resource = resource; }
    }
    public static final String STYLE = "appearance_button_style";
    public static final String SHAPE = "appearance_button_shape";
    public static final String FONT = "appearance_text_font";
    private Appearance() {}
    public static boolean isPreference(String key) {
        return STYLE.equals(key) || SHAPE.equals(key) || FONT.equals(key);
    }
    private static <T extends Enum<T>> T read(Context context, String key, T fallback) {
        Object raw = Preferences.getInstance(context).getPreferences().getAll().get(key);
        try { return Enum.valueOf(fallback.getDeclaringClass(), String.valueOf(raw)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
    public static Style style(Context c) { return read(c, STYLE, Style.FLAT); }
    public static Shape shape(Context c) { return read(c, SHAPE, Shape.ROUNDED); }
    public static Font font(Context c) { return read(c, FONT, Font.OSWALD); }
    public static void applyButtons(Context c, Style style, Shape shape) {
        Preferences.getInstance(c).getPreferences().edit()
                .putString(STYLE, style.name()).putString(SHAPE, shape.name()).apply();
    }
    public static void applyFont(Context c, Font font) {
        Preferences.getInstance(c).getPreferences().edit().putString(FONT, font.name()).apply();
    }
    public static void reset(Context c) {
        Preferences.getInstance(c).getPreferences().edit().remove(STYLE).remove(SHAPE).remove(FONT).apply();
    }
}
