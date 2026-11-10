package net.smartlogic.unitconverter.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.json.JSONObject;

/** Raw input survives incomplete edits. Disabled amount/percent modes retain independent values. */
public final class InvestmentDraft {
    public final InvestmentInputs.Kind kind;
    public String amount = "", rate = "", tenure = "", stepAmount = "0", stepPercent = "0", currency;
    public boolean years = true, stepEnabled, percent = true, periodic;
    public int frequency = 3, stepMonths = 12;
    public LocalDate start = LocalDate.now();
    public InvestmentDraft(InvestmentInputs.Kind kind, String currency) { this.kind = kind; this.currency = currency; }
    public String stepText() { return percent ? stepPercent : stepAmount; }
    public InvestmentInputs inputs() {
        BigDecimal term = decimal(tenure, "tenure");
        int months;
        try { months = term.multiply(BigDecimal.valueOf(years ? 12 : 1)).intValueExact(); }
        catch (ArithmeticException error) { throw new IllegalArgumentException("tenure"); }
        return new InvestmentInputs(kind, decimal(amount, "amount"), decimal(rate, "rate"), months,
                currency, start, frequency, periodic, stepEnabled, percent, stepEnabled ? decimal(stepText(), "step") : BigDecimal.ZERO, stepMonths);
    }
    private static BigDecimal decimal(String text, String field) {
        if (text == null || text.length() > 20 || !text.matches("-?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)")) throw new IllegalArgumentException(field);
        try { return new BigDecimal(text); } catch (NumberFormatException error) { throw new IllegalArgumentException(field); }
    }
    public String json() {
        try { return new JSONObject().put("version", 1).put("kind", kind.name()).put("currency", currency)
                .put("amount", amount).put("rate", rate).put("tenure", tenure).put("years", years)
                .put("stepAmount", stepAmount).put("stepPercent", stepPercent).put("percent", percent)
                .put("stepEnabled", stepEnabled).put("stepMonths", stepMonths).put("frequency", frequency)
                .put("periodic", periodic).put("start", start.toString()).toString();
        } catch (org.json.JSONException error) { throw new IllegalStateException(error); }
    }
    public static InvestmentDraft read(String text, InvestmentInputs.Kind expected) {
        try {
            if (text == null || text.length() > 4096) throw new IllegalArgumentException("state");
            JSONObject o = new JSONObject(text);
            if (o.getInt("version") != 1 || !o.getString("kind").equals(expected.name())) throw new IllegalArgumentException("state");
            String currency = o.getString("currency");
            // Only currencies actually offered by Settings are valid persisted choices.
            if (java.util.Currency.getInstance(currency).getDefaultFractionDigits() < 0) throw new IllegalArgumentException("currency");
            InvestmentDraft d = new InvestmentDraft(expected, currency);
            d.amount = raw(o, "amount"); d.rate = raw(o, "rate"); d.tenure = raw(o, "tenure");
            d.stepAmount = raw(o, "stepAmount"); d.stepPercent = raw(o, "stepPercent");
            d.years = o.getBoolean("years"); d.percent = o.getBoolean("percent"); d.stepEnabled = o.getBoolean("stepEnabled");
            d.stepMonths = o.getInt("stepMonths"); d.frequency = o.getInt("frequency"); d.periodic = o.getBoolean("periodic");
            d.start = LocalDate.parse(o.getString("start"));
            if (d.start.getYear() < 1900 || d.start.getYear() > 2100 || !java.util.Arrays.asList(3,6,12).contains(d.stepMonths)
                    || !java.util.Arrays.asList(1,3,6,12).contains(d.frequency)) throw new IllegalArgumentException("state");
            return d;
        } catch (org.json.JSONException error) { throw new IllegalArgumentException("state", error); }
    }
    private static String raw(JSONObject o, String key) throws org.json.JSONException {
        String value = o.getString(key);
        if (value.length() > 20 || !value.matches("-?[0-9]*(?:\\.[0-9]*)?")) throw new IllegalArgumentException("state");
        return value;
    }
}
