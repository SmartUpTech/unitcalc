package net.smartlogic.unitconverter.calculator.finance.emi;

import org.json.JSONArray;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.smartlogic.unitconverter.utils.NumberUtils;

/** Raw editor values are distinct from the immutable validated calculation. */
public final class EmiDraft {
    public String principal = "", rate = "", tenure = "20", currency;
    public boolean years = true;
    public LocalDate firstEmi = LocalDate.now().plusMonths(1);
    public EmiInputs.Method method = EmiInputs.Method.REDUCE_TENURE;
    public final List<PrepaymentRule> rules = new ArrayList<>();
    public EmiDraft(String currency) { this.currency = currency; }
    public EmiInputs inputs() {
        BigDecimal term = NumberUtils.parseDecimal(tenure);
        int months = (years ? term.multiply(new BigDecimal("12")) : term).intValueExact();
        return new EmiInputs(NumberUtils.parseDecimal(principal), NumberUtils.parseDecimal(rate), months, firstEmi, currency, method, rules);
    }
    public JSONObject json() {
        try {
            JSONArray list = new JSONArray();
            for (PrepaymentRule r : rules) list.put(new JSONObject().put("amount", r.amount().toPlainString())
                    .put("first", r.firstMonth()).put("last", r.lastMonth()).put("interval", r.interval()));
            return new JSONObject().put("version", 1).put("principal", principal).put("rate", rate).put("tenure", tenure)
                    .put("years", years).put("currency", currency).put("date", firstEmi.toString()).put("method", method.name()).put("rules", list);
        } catch (org.json.JSONException impossible) { throw new IllegalStateException(impossible); }
    }
    public static EmiDraft read(String json) {
        try {
            if (json == null || json.length() > 32768) throw new IllegalArgumentException("state");
            JSONObject o = new JSONObject(json);
            if (o.getInt("version") != 1) throw new IllegalArgumentException("version");
            EmiDraft d = new EmiDraft(o.getString("currency"));
            java.util.Currency.getInstance(d.currency);
            d.principal = o.getString("principal"); d.rate = o.getString("rate"); d.tenure = o.getString("tenure");
            if (d.principal.length() > 18 || d.rate.length() > 8 || d.tenure.length() > 10) throw new IllegalArgumentException("state");
            d.years = o.getBoolean("years"); d.firstEmi = LocalDate.parse(o.getString("date"));
            if (d.firstEmi.getYear() < 1900 || d.firstEmi.getYear() > 2100) throw new IllegalArgumentException("date");
            d.method = EmiInputs.Method.valueOf(o.getString("method"));
            JSONArray rows = o.getJSONArray("rules");
            if (rows.length() > EmiCalculator.MAX_RULES) throw new IllegalArgumentException("rules");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject r = rows.getJSONObject(i);
                d.rules.add(new PrepaymentRule(NumberUtils.parseDecimal(r.getString("amount")), r.getInt("first"), r.getInt("last"), r.getInt("interval")));
            }
            return d;
        } catch (org.json.JSONException e) { throw new IllegalArgumentException("state", e); }
    }
}
