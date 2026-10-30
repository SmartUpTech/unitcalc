package net.smartlogic.unitconverter.theme;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.ColorUtils;
import net.smartlogic.unitconverter.R;

/** One renderer for real keys and customization previews. Geometry never changes touch bounds. */
public final class KeyAppearanceDrawable extends Drawable {
    public enum Role { NUMBER, FUNCTION, OPERATOR, UTILITY, EQUAL }
    private final Appearance.Style style;
    private final Appearance.Shape shape;
    private final CalculatorTheme theme;
    private final Role role;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float gap, radius, edge;
    public KeyAppearanceDrawable(Resources r, CalculatorTheme theme, Appearance.Style style,
            Appearance.Shape shape, Role role) {
        this.theme = theme; this.style = style; this.shape = shape; this.role = role;
        gap = r.getDimension(R.dimen.appearance_key_gap);
        radius = r.getDimension(R.dimen.appearance_key_radius);
        edge = r.getDimension(R.dimen.theme_outline);
    }
    private static int blend(int from, int to, float fraction) {
        // Float interpolation can round an opaque alpha down to 254.
        return ColorUtils.setAlphaComponent(ColorUtils.blendARGB(from, to, fraction), 255);
    }
    public int surface() {
        if (role == Role.EQUAL) return theme.accentContentColor();
        if (style == Appearance.Style.FLAT || style == Appearance.Style.OUTLINE) return theme.background();
        int tint = style == Appearance.Style.TONAL ? switch (role) {
            case NUMBER -> theme.primaryContentColor();
            case FUNCTION, UTILITY -> theme.secondaryContentColor();
            default -> theme.operatorContentColor();
        } : theme.primaryContentColor();
        float amount = style == Appearance.Style.TONAL ? (role == Role.NUMBER ? .06f : .16f) : .08f;
        return blend(theme.background(), tint, amount);
    }
    public int foreground() {
        int preferred = switch (role) {
            case NUMBER, UTILITY -> theme.primaryContentColor();
            case FUNCTION -> theme.secondaryContentColor();
            case OPERATOR -> theme.operatorContentColor();
            case EQUAL -> theme.onAccentColor();
        };
        return CalculatorTheme.readableColor(preferred, surface());
    }
    private int finishTint(int target, float amount) {
        int fill = surface(), ink = foreground();
        int tinted = blend(fill, target, amount);
        while (amount > .001f && ColorUtils.calculateContrast(ink, tinted) < 4.5) {
            amount *= .5f;
            tinted = blend(fill, target, amount);
        }
        return ColorUtils.calculateContrast(ink, tinted) >= 4.5 ? tinted : fill;
    }
    private Path path(RectF b) {
        Path p = new Path();
        switch (shape) {
            case CIRCLE -> {
                float half = Math.min(b.width(), b.height()) / 2;
                p.addCircle(b.centerX(), b.centerY(), half, Path.Direction.CW);
            }
            case SQUARE -> p.addRoundRect(b, edge, edge, Path.Direction.CW);
            case ROUNDED -> p.addRoundRect(b, radius, radius, Path.Direction.CW);
            case SQUIRCLE -> {
                // Superellipse: continuous curvature, unlike circular rounded-rectangle corners.
                for (int i = 0; i <= 96; i++) {
                    double a = i * Math.PI * 2 / 96, x = Math.cos(a), y = Math.sin(a);
                    float px = b.centerX() + b.width()/2 * (float)(Math.signum(x)*Math.sqrt(Math.abs(x)));
                    float py = b.centerY() + b.height()/2 * (float)(Math.signum(y)*Math.sqrt(Math.abs(y)));
                    if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
                }
                p.close();
            }
        }
        return p;
    }
    @Override public void draw(Canvas c) {
        if (style == Appearance.Style.FLAT && role != Role.EQUAL) return;
        RectF b = new RectF(getBounds()); b.inset(gap, gap);
        if (b.width() <= 0 || b.height() <= 0) return;
        Path p = path(b);
        int fill = surface();
        paint.setShader(null); paint.setStyle(Paint.Style.FILL);
        if (style == Appearance.Style.RAISED) {
            c.save(); c.translate(0, edge * 2);
            paint.setColor(blend(theme.background(), theme.primaryContentColor(), .22f));
            c.drawPath(p, paint); c.restore();
        }
        paint.setColor(fill);
        if (style == Appearance.Style.SATIN || style == Appearance.Style.BEVEL || style == Appearance.Style.INSET) {
            boolean inset = style == Appearance.Style.INSET;
            int top = finishTint(inset ? Color.BLACK : Color.WHITE, .10f);
            int bottom = finishTint(inset ? Color.WHITE : Color.BLACK, .08f);
            paint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{top, fill, bottom}, new float[]{0, .45f, 1}, Shader.TileMode.CLAMP));
        }
        if (style != Appearance.Style.OUTLINE || role == Role.EQUAL) c.drawPath(p, paint);
        paint.setShader(null);
        if (style == Appearance.Style.OUTLINE || style == Appearance.Style.BEVEL || style == Appearance.Style.INSET) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(style == Appearance.Style.BEVEL ? edge * 2 : edge);
            paint.setColor(blend(fill, foreground(), style == Appearance.Style.OUTLINE ? .6f : .25f));
            c.drawPath(p, paint);
            if (style == Appearance.Style.BEVEL || style == Appearance.Style.INSET) {
                c.save(); c.clipPath(p); c.translate(0, style == Appearance.Style.INSET ? edge : -edge);
                paint.setColor(blend(fill, Color.WHITE, .3f));
                c.drawPath(p, paint); c.restore();
            }
        }
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
