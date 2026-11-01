package net.smartlogic.unitconverter.emi;

import android.content.Context;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.graphy.model.*;

/** Only semantic dependencies and authoritative values; layout belongs to Graphy. */
public final class EmiGraphBuilder {
    private EmiGraphBuilder() { }
    public static GraphyOutput build(Context c, EmiResult r, boolean prepayment) { return build(c, r, prepayment, false); }
    public static GraphyOutput build(Context c, EmiResult r, boolean prepayment, boolean years) {
        List<GraphyNode> nodes = new ArrayList<>(); List<GraphyConnection> edges = new ArrayList<>();
        EmiFormatter f = new EmiFormatter(c); String notice = c.getString(R.string.emi_assumptions);
        nodes.add(node(c, "p", GraphyNodeType.INPUT, R.string.emi_loan_amount, f.money(r.inputs().principal(), r), null, notice));
        nodes.add(node(c, "annual", GraphyNodeType.INPUT, R.string.emi_annual_rate, c.getString(R.string.emi_rate_value, r.inputs().annualRate().toPlainString()), null, notice));
        String tenure = years ? c.getString(R.string.emi_term_value, new BigDecimal(r.inputs().months()).divide(new BigDecimal("12"), java.math.MathContext.DECIMAL128).stripTrailingZeros().toPlainString(), c.getString(R.string.emi_years)) : c.getString(R.string.emi_payments_value, r.inputs().months());
        nodes.add(node(c, "term_input", GraphyNodeType.INPUT, R.string.emi_tenure, tenure, null, notice));
        if (years) {
            nodes.add(node(c, "months_op", GraphyNodeType.OPERATION, R.string.emi_term_step, "× 12", null, notice));
            nodes.add(node(c, "term", GraphyNodeType.DERIVED, R.string.emi_payment_count, c.getString(R.string.emi_payments_value, r.inputs().months()), null, notice));
            edges.add(edge("term_input", "months_op")); edges.add(edge("months_op", "term"));
        }
        String value;
        if (prepayment && !r.inputs().prepayments().isEmpty()) {
            String rule = c.getString(R.string.emi_graph_extra_detail);
            nodes.add(node(c, "original", GraphyNodeType.OPERATION, R.string.emi_original, c.getString(R.string.emi_operation_schedule), null, notice));
            nodes.add(node(c, "base_interest", GraphyNodeType.DERIVED, R.string.emi_baseline_interest, f.money(r.baselineInterest(), r), null, notice));
            nodes.add(node(c, "extra", GraphyNodeType.INPUT, R.string.emi_extra, f.money(r.totalPrepayment(), r), null, rule));
            nodes.add(node(c, "revised", GraphyNodeType.OPERATION, R.string.emi_revised, c.getString(R.string.emi_apply_extra), null, rule));
            nodes.add(node(c, "new_interest", GraphyNodeType.DERIVED, R.string.emi_revised_interest, f.money(r.totalInterest(), r), null, rule));
            nodes.add(node(c, "subtract", GraphyNodeType.OPERATION, R.string.emi_saved_interest, "−", null, rule));
            value = f.money(r.interestSaved(), r);
            nodes.add(node(c, "result", GraphyNodeType.RESULT, R.string.emi_saved_interest, value, null, rule));
            for (String id : java.util.Arrays.asList("p", "annual", "term")) { edges.add(edge(id.equals("term") && !years ? "term_input" : id, "original")); edges.add(edge(id.equals("term") && !years ? "term_input" : id, "revised")); }
            edges.addAll(java.util.Arrays.asList(edge("original", "base_interest"), edge("extra", "revised"), edge("revised", "new_interest"),
                    edge("base_interest", "subtract"), edge("new_interest", "subtract"), edge("subtract", "result")));
        } else {
            String formula = c.getString(R.string.emi_formula_detail);
            nodes.add(node(c, "rate_op", GraphyNodeType.OPERATION, R.string.emi_rate_step, "÷ 12", formula, notice));
            nodes.add(node(c, "rate", GraphyNodeType.DERIVED, R.string.emi_monthly_rate, c.getString(R.string.emi_rate_value,
                    net.smartlogic.unitconverter.utils.NumberUtils.formatCalculator(r.monthlyRate().multiply(new BigDecimal("100")), 8)), null, notice));
            nodes.add(node(c, "formula", GraphyNodeType.OPERATION, R.string.emi_formula, c.getString(R.string.emi_formula), formula, notice));
            value = f.money(r.regularEmi(), r);
            nodes.add(node(c, "result", GraphyNodeType.RESULT, R.string.emi_monthly, value, formula, notice));
            edges.addAll(java.util.Arrays.asList(edge("annual", "rate_op"), edge("rate_op", "rate"), edge("rate", "formula"), edge("p", "formula"), edge(years ? "term" : "term_input", "formula"), edge("formula", "result")));
        }
        return GraphyOutput.create(CalculatorCatalog.ID_EMI, c.getString(prepayment ? R.string.emi_graph_prepay : R.string.emi_graph_formula), value,
                nodes, edges, notice, java.util.Collections.singletonMap(GraphyOutput.METADATA_NOTICE, notice));
    }
    private static GraphyNode node(Context c, String id, GraphyNodeType type, int label, String value, String formula, String description) {
        return new GraphyNode(id, type, c.getString(label), value, type.name().toLowerCase(java.util.Locale.ROOT), formula, description);
    }
    private static GraphyConnection edge(String a, String b) { return new GraphyConnection(a, b, null); }
}
