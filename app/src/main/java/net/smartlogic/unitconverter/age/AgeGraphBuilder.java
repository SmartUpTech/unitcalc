package net.smartlogic.unitconverter.age;

import android.content.Context;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.model.GraphyConnection;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import java.util.Arrays;
import java.util.Map;

/** Semantic date explanation only. The shared engine owns all drawing and layout. */
public final class AgeGraphBuilder {
    private AgeGraphBuilder() { }

    public static GraphyOutput build(Context context, AgeCalculator.Result r) {
        AgeFormatter f = new AgeFormatter(context);
        String rule = context.getString(R.string.age_calendar_rule);
        String years = context.getString(R.string.age_year_step, f.years(r.age().years()), f.date(r.age().anniversary()));
        String months = context.getString(R.string.age_month_step, f.months(r.age().months()), f.date(r.age().monthAnchor()));
        String days = context.getString(R.string.age_day_step, f.date(r.age().monthAnchor()),
                f.date(r.asOf()), f.days(r.age().days()));
        String interval = context.getString(R.string.age_date_interval, f.date(r.birth()), f.date(r.asOf()));
        return GraphyOutput.create(CalculatorCatalog.ID_AGE, interval, f.breakdown(r.age()), Arrays.asList(
                new GraphyNode("birth", GraphyNodeType.INPUT, context.getString(R.string.age_birth), f.date(r.birth()), "input"),
                new GraphyNode("asof", GraphyNodeType.INPUT, context.getString(R.string.age_as_of), f.date(r.asOf()), "input"),
                new GraphyNode("years_op", GraphyNodeType.OPERATION, context.getString(R.string.age_completed_years), context.getString(R.string.age_graphy_step_years), "operation", years, rule),
                new GraphyNode("years", GraphyNodeType.DERIVED, context.getString(R.string.age_anniversary), years, "derived", years, rule),
                new GraphyNode("months_op", GraphyNodeType.OPERATION, context.getString(R.string.age_completed_months), context.getString(R.string.age_graphy_step_months), "operation", months, rule),
                new GraphyNode("months", GraphyNodeType.DERIVED, context.getString(R.string.age_month_anchor), months, "derived", months, rule),
                new GraphyNode("days_op", GraphyNodeType.OPERATION, context.getString(R.string.age_remaining_days), context.getString(R.string.age_graphy_step_days), "operation", days, rule),
                new GraphyNode("days", GraphyNodeType.DERIVED, context.getString(R.string.age_remaining_days), f.days(r.age().days()), "derived", days, rule),
                new GraphyNode("age", GraphyNodeType.RESULT, context.getString(R.string.age_result), f.breakdown(r.age()), "result", null, rule)
        ), Arrays.asList(
                edge("birth", "years_op"), edge("asof", "years_op"), edge("years_op", "years"),
                edge("years", "months_op"), edge("months_op", "months"),
                edge("months", "days_op"), edge("days_op", "days"),
                edge("years", "age"), edge("months", "age"), edge("days", "age")
        ), rule, Map.of(GraphyOutput.METADATA_CONTEXT, interval));
    }

    private static GraphyConnection edge(String from, String to) { return new GraphyConnection(from, to, null); }
}
