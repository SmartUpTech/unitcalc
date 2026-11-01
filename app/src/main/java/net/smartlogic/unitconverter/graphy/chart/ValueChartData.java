package net.smartlogic.unitconverter.graphy.chart;

import java.math.BigDecimal;
import java.util.List;

/** Content-only shared chart model; all labels are already formatted by the calculation. */
public record ValueChartData(Kind kind, List<Series> series, String xStart, String xEnd, String yMax, String yZero) {
    public ValueChartData(Kind kind, List<Series> series) { this(kind, series, "", "", "", ""); }
    public enum Kind { PROPORTIONS, LINE }
    public record Point(BigDecimal value, String label) { }
    public record Series(String label, List<Point> points) { public Series { points = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(points)); } }
    public ValueChartData { series = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(series)); }
    public String description() {
        StringBuilder text = new StringBuilder();
        for (Series s : series) {
            text.append(s.label()).append('\n');
            if (!s.points().isEmpty()) text.append(s.points().get(0).label()).append('\n');
            if (s.points().size() > 1) text.append(s.points().get(s.points().size() - 1).label()).append('\n');
        }
        return text.toString();
    }
}
