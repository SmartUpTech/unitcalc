package net.smartlogic.unitconverter.calculator.finance.investment;

import android.content.Context;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiFormatter;
import net.smartlogic.unitconverter.graphy.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Content only: all visual decisions remain in the shared Graphy engine. */
public final class InvestmentGraphBuilder {
    private InvestmentGraphBuilder() { }
    public static GraphyOutput build(Context c, InvestmentResult r) {
        var i = r.inputs(); var f = new EmiFormatter(c);
        List<GraphyNode> nodes = new ArrayList<>(); List<GraphyConnection> edges = new ArrayList<>();
        String notice = c.getString(R.string.investment_assumptions);
        String detail = c.getString(i.kind() == InvestmentInputs.Kind.SIP ? R.string.investment_sip_assumptions
                : i.kind() == InvestmentInputs.Kind.RD ? R.string.investment_rd_assumptions : R.string.investment_fd_assumptions);
        add(nodes, "amount", GraphyNodeType.INPUT, c.getString(InvestmentCalculatorFragment.amountLabel(i.kind())), f.money(i.amount(), i.currency()), null, detail);
        add(nodes, "rate", GraphyNodeType.INPUT, c.getString(R.string.emi_annual_rate), c.getString(R.string.emi_rate_value, i.annualRate().toPlainString()), null, detail);
        add(nodes, "months", GraphyNodeType.INPUT, c.getString(R.string.emi_tenure), c.getString(R.string.emi_payments_value, i.months()), null, detail);
        if (i.kind() != InvestmentInputs.Kind.SIP) {
            add(nodes, "date", GraphyNodeType.INPUT, c.getString(R.string.investment_start), f.date(i.start()), null, notice);
            edges.add(new GraphyConnection("date", "grow", null));
        }
        String source = "amount";
        if (i.stepEnabled()) {
            String value = i.stepPercent() ? c.getString(R.string.emi_rate_value, i.step().toPlainString()) : f.money(i.step(), i.currency());
            add(nodes, "step", GraphyNodeType.INPUT, c.getString(R.string.investment_step), c.getString(R.string.investment_step_value, value, i.stepMonths()), null, detail);
            add(nodes, "step_op", GraphyNodeType.OPERATION, c.getString(R.string.investment_step), c.getString(R.string.investment_step_operation, i.stepMonths()), null,
                    c.getString(R.string.investment_step_description, value, i.stepMonths()));
            add(nodes, "invested", GraphyNodeType.DERIVED, c.getString(R.string.investment_invested), f.money(r.invested(), i.currency()), null, detail);
            edges.add(new GraphyConnection("amount", "step_op", null)); edges.add(new GraphyConnection("step", "step_op", null));
            edges.add(new GraphyConnection("months", "step_op", null)); edges.add(new GraphyConnection("step_op", "invested", null)); source = "invested";
        }
        if (i.kind() == InvestmentInputs.Kind.FD) {
            add(nodes, "frequency", GraphyNodeType.INPUT, c.getString(i.periodic() ? R.string.investment_payout_frequency : R.string.investment_compounding),
                    c.getString(InvestmentCalculatorFragment.frequencyLabel(i.frequency())), null, detail);
            add(nodes, "mode", GraphyNodeType.INPUT, c.getString(R.string.investment_payout_mode), c.getString(i.periodic() ? R.string.investment_periodic : R.string.investment_cumulative), null, detail);
            edges.add(new GraphyConnection("frequency", "grow", null)); edges.add(new GraphyConnection("mode", "grow", null));
        }
        add(nodes, "grow", GraphyNodeType.OPERATION, c.getString(R.string.investment_interest), c.getString(i.kind() == InvestmentInputs.Kind.FD
                ? R.string.investment_fd_operation : R.string.investment_growth_operation), c.getString(i.kind() == InvestmentInputs.Kind.SIP
                ? R.string.investment_formula_sip : i.kind() == InvestmentInputs.Kind.RD ? R.string.investment_formula_rd : R.string.investment_formula_fd), detail);
        edges.add(new GraphyConnection(source, "grow", null)); edges.add(new GraphyConnection("rate", "grow", null)); edges.add(new GraphyConnection("months", "grow", null));
        add(nodes, "interest", GraphyNodeType.DERIVED, c.getString(R.string.investment_interest), f.money(r.interest(), i.currency()), null, detail);
        edges.add(new GraphyConnection("grow", "interest", null));
        if (i.periodic()) {
            add(nodes, "receipts", GraphyNodeType.DERIVED, c.getString(R.string.investment_receipts), f.money(r.receipts(), i.currency()), null, detail);
            edges.add(new GraphyConnection("interest", "receipts", null));
        }
        String value = f.money(r.maturity(), i.currency());
        add(nodes, "result", GraphyNodeType.RESULT, c.getString(i.kind() == InvestmentInputs.Kind.SIP ? R.string.investment_future : R.string.investment_maturity), value, null, detail);
        edges.add(new GraphyConnection("grow", "result", null));
        return GraphyOutput.create(InvestmentCalculatorFragment.toolId(i.kind()), c.getString(InvestmentCalculatorFragment.title(i.kind())), value,
                nodes, edges, detail, java.util.Collections.singletonMap(GraphyOutput.METADATA_NOTICE, c.getString(i.kind() == InvestmentInputs.Kind.SIP
                        ? R.string.investment_sip_notice : i.kind() == InvestmentInputs.Kind.RD ? R.string.investment_rd_notice
                        : i.periodic() ? R.string.investment_periodic_notice : R.string.investment_estimate)));
    }
    private static void add(List<GraphyNode> nodes, String id, GraphyNodeType type, String label, String value, String formula, String description) {
        nodes.add(new GraphyNode(id, type, label, value, type.name().toLowerCase(Locale.ROOT), formula, description));
    }
}
