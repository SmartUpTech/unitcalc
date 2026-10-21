package net.smartlogic.unitconverter.helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Validated USD snapshots. Legacy caches remain readable until a successful refresh. */
public final class CurrencyRateStore {
    public static final String ENDPOINT = "https://api.frankfurter.dev/v2/rates?base=USD";
    public static final long REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(12);

    public interface Fetcher { String fetch() throws Exception; }

    public static final class Snapshot {
        public final Map<String, Float> rates;
        public final Map<String, String> dates;
        public final String provider;
        public final long fetchedAt;

        private Snapshot(Map<String, Float> rates, Map<String, String> dates, String provider, long fetchedAt) {
            this.rates = Collections.unmodifiableMap(new HashMap<>(rates));
            this.dates = Collections.unmodifiableMap(new HashMap<>(dates));
            this.provider = provider;
            this.fetchedAt = fetchedAt;
        }

        public boolean isFresh(long now) {
            return fetchedAt > 0 && now >= fetchedAt && now - fetchedAt < REFRESH_INTERVAL;
        }

        public String encode() throws JSONException {
            return new JSONObject().put("provider", provider).put("base", "USD")
                    .put("fetchedAt", fetchedAt).put("rates", new JSONObject(rates))
                    .put("dates", new JSONObject(dates)).toString();
        }
    }

    public static Snapshot parseResponse(String body, long fetchedAt) throws JSONException {
        if (body == null || body.trim().isEmpty()) throw new JSONException("Empty rate response");
        JSONArray rows = new JSONArray(body);
        if (rows.length() == 0) throw new JSONException("No rates");
        Map<String, Float> rates = new HashMap<>();
        Map<String, String> dates = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String quote = row.getString("quote");
            if (!"USD".equals(row.getString("base")) || !quote.matches("[A-Z]{3}")
                    || rates.containsKey(quote)) throw new JSONException("Invalid currency identity");
            String date = row.getString("date");
            try { LocalDate.parse(date); }
            catch (RuntimeException invalid) { throw new JSONException("Invalid rate date"); }
            rates.put(quote, validRate(row.getDouble("rate")));
            dates.put(quote, date);
        }
        if (rates.containsKey("USD") && rates.get("USD") != 1f) throw new JSONException("Invalid base rate");
        rates.put("USD", 1f); // Identity, not a fallback exchange rate.
        if (rates.size() < 2) throw new JSONException("No quote rates");
        return new Snapshot(rates, dates, "Frankfurter", fetchedAt);
    }

    public static Snapshot readCache(String body, long legacyTime) {
        if (body == null || body.trim().isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(body);
            boolean normalized = root.has("provider");
            if (normalized && !"USD".equals(root.getString("base"))) return null;
            JSONObject data = root.has("data") ? root.getJSONObject("data") : root.getJSONObject("rates");
            Map<String, Float> rates = new HashMap<>();
            Map<String, String> dates = new HashMap<>();
            for (Iterator<String> keys = data.keys(); keys.hasNext();) {
                String key = keys.next();
                if (!key.matches("[A-Z]{3}")) return null;
                rates.put(key, validRate(data.getDouble(key)));
                if (normalized && !"USD".equals(key)) {
                    String date = root.getJSONObject("dates").getString(key);
                    LocalDate.parse(date);
                    dates.put(key, date);
                }
            }
            if (rates.size() < 2 || !rates.containsKey("USD") || rates.get("USD") != 1f) return null;
            return new Snapshot(rates, dates, normalized ? root.getString("provider") : "Freecurrencyapi",
                    normalized ? root.getLong("fetchedAt") : legacyTime);
        } catch (JSONException | RuntimeException invalid) { return null; }
    }

    private static float validRate(double value) throws JSONException {
        float rate = (float) value;
        if (Float.isNaN(rate) || Float.isInfinite(rate) || rate <= 0) throw new JSONException("Invalid rate");
        return rate;
    }

    /** Failed fetches never write preferences or replace the last valid snapshot. */
    public static Snapshot refresh(Preferences prefs, boolean forced, long now, Fetcher fetcher) throws Exception {
        Snapshot cached = readCache(prefs.getCurrencyResponse(), prefs.getCurrencyLastUpdateDate());
        if (!forced && cached != null && "Frankfurter".equals(cached.provider) && cached.isFresh(now)) return cached;
        Snapshot fresh = parseResponse(fetcher.fetch(), now);
        prefs.setCurrencySnapshot(fresh.encode(), fresh.fetchedAt);
        return fresh;
    }

    private CurrencyRateStore() { }
}
