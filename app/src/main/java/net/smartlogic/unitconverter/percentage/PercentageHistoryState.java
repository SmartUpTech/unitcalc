package net.smartlogic.unitconverter.percentage;

import androidx.annotation.Nullable;
import org.json.JSONObject;

/** Versioned draft/history payload for the percentage calculator. */
public final class PercentageHistoryState {
    public static final int VERSION = 1;

    public final PercentageCalculator.Mode mode;
    public final String first;
    public final String second;
    public final int focus;

    public PercentageHistoryState(
            PercentageCalculator.Mode mode,
            String first,
            String second,
            int focus) {
        this.mode = mode == null ? PercentageCalculator.Mode.PERCENT_OF : mode;
        this.first = first == null ? "" : first;
        this.second = second == null ? "" : second;
        this.focus = Math.max(0, Math.min(1, focus));
    }

    public String toPayload() {
        try {
            return new JSONObject()
                    .put("version", VERSION)
                    .put("mode", mode.name())
                    .put("first", first)
                    .put("second", second)
                    .put("focus", focus)
                    .toString();
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @Nullable public static PercentageHistoryState fromPayload(@Nullable String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            JSONObject json = new JSONObject(payload);
            if (json.optInt("version", 0) != VERSION) return null;
            PercentageCalculator.Mode mode = PercentageCalculator.Mode.valueOf(json.getString("mode"));
            return new PercentageHistoryState(
                    mode,
                    json.optString("first"),
                    json.optString("second"),
                    json.optInt("focus", 0));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable public static PercentageHistoryState fromDraft(@Nullable String draft) {
        return fromPayload(draft);
    }

    /** Best-effort parse of history expression lines like {@code Increase · 250 · 12%}. */
    @Nullable public static PercentageHistoryState fromLegacyExpression(@Nullable String expression) {
        if (expression == null || expression.isBlank()) return null;
        String[] parts = expression.split(" · ");
        if (parts.length < 3) return null;
        PercentageCalculator.Mode mode = switch (parts[0].trim()) {
            case "Percent of", "Percent Of" -> PercentageCalculator.Mode.PERCENT_OF;
            case "Increase" -> PercentageCalculator.Mode.INCREASE;
            case "Decrease" -> PercentageCalculator.Mode.DECREASE;
            case "What %", "What percent" -> PercentageCalculator.Mode.WHAT_PERCENT;
            default -> null;
        };
        if (mode == null) return null;
        String first = digits(parts[1]);
        String second = digits(parts[2]);
        if (first.isEmpty() || second.isEmpty()) return null;
        return new PercentageHistoryState(mode, first, second, 0);
    }

    private static String digits(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        boolean seenDot = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') out.append(c);
            else if (c == '.' && !seenDot) {
                out.append(c);
                seenDot = true;
            } else if (c == '-' && out.length() == 0) out.append(c);
        }
        return out.toString();
    }
}
