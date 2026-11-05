package net.smartlogic.unitconverter.fuel;

import androidx.annotation.Nullable;
import net.smartlogic.unitconverter.model.Unit;
import org.json.JSONObject;

/** Serializable fuel calculator inputs for history payload and legacy expression fallback. */
public final class FuelHistoryState {
    public static final int VERSION = 1;

    public final String distance;
    public final String efficiency;
    public final String price;
    public final String passengers;
    public final int distanceUnitId;
    public final int efficiencyUnitId;
    public final boolean roundTrip;

    public FuelHistoryState(
            String distance,
            String efficiency,
            String price,
            String passengers,
            int distanceUnitId,
            int efficiencyUnitId,
            boolean roundTrip) {
        this.distance = distance == null ? "" : distance;
        this.efficiency = efficiency == null ? "" : efficiency;
        this.price = price == null ? "" : price;
        this.passengers = passengers == null || passengers.isBlank() ? "1" : passengers;
        this.distanceUnitId = distanceUnitId;
        this.efficiencyUnitId = efficiencyUnitId;
        this.roundTrip = roundTrip;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("version", VERSION)
                    .put("distance", distance)
                    .put("efficiency", efficiency)
                    .put("price", price)
                    .put("passengers", passengers)
                    .put("distanceUnit", distanceUnitId)
                    .put("efficiencyUnit", efficiencyUnitId)
                    .put("roundTrip", roundTrip);
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public String toPayload() {
        return toJson().toString();
    }

    @Nullable public static FuelHistoryState fromPayload(@Nullable String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            JSONObject json = new JSONObject(payload);
            if (json.optInt("version", 0) != VERSION) return null;
            return fromJson(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Reads draft JSON saved in preferences (no version field). */
    @Nullable public static FuelHistoryState fromDraft(@Nullable String draft) {
        if (draft == null || draft.isBlank()) return null;
        try {
            return fromJson(new JSONObject(draft));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable public static FuelHistoryState fromLegacyExpression(@Nullable String expression) {
        if (expression == null || expression.isBlank()) return null;
        String[] parts = expression.split(" · ");
        if (parts.length == 0) return null;
        String distance = parts[0].trim();
        String efficiency = "";
        String price = "";
        String passengers = "1";
        boolean roundTrip = expression.contains(" · RT") || expression.endsWith("RT");
        if (parts.length >= 2) {
            String efficiencyPart = parts[1].trim();
            int split = efficiencyPart.lastIndexOf(' ');
            efficiency = split > 0 ? efficiencyPart.substring(0, split).trim() : efficiencyPart;
        }
        if (parts.length >= 3) {
            price = parseLegacyPrice(parts[2]);
        }
        if (parts.length >= 4) {
            String pax = parts[3].trim();
            int space = pax.indexOf(' ');
            passengers = space > 0 ? pax.substring(0, space).trim() : pax.replaceAll("\\D", "");
            if (passengers.isEmpty()) passengers = "1";
        }
        return new FuelHistoryState(
                distance,
                efficiency,
                price,
                passengers,
                Unit.KILOMETRE,
                Unit.L_100K,
                roundTrip);
    }

    private static FuelHistoryState fromJson(JSONObject json) {
        return new FuelHistoryState(
                json.optString("distance"),
                json.optString("efficiency"),
                json.optString("price"),
                json.optString("passengers", "1"),
                json.optInt("distanceUnit", Unit.KILOMETRE),
                json.optInt("efficiencyUnit", Unit.L_100K),
                json.optBoolean("roundTrip"));
    }

    /** Best-effort digits from a formatted `{symbol}102.50/L` history segment. */
    private static String parseLegacyPrice(String segment) {
        if (segment == null) return "";
        String text = segment.trim();
        if (text.endsWith("/L")) text = text.substring(0, text.length() - 2).trim();
        StringBuilder digits = new StringBuilder();
        boolean seenDot = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
            else if (c == '.' && !seenDot) {
                digits.append(c);
                seenDot = true;
            }
        }
        return digits.toString();
    }
}
