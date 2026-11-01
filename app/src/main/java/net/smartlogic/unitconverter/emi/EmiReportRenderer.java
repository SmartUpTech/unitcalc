package net.smartlogic.unitconverter.emi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.chart.ValueChartRenderer;
import net.smartlogic.unitconverter.theme.AppearanceTypography;
import net.smartlogic.unitconverter.theme.CalculatorThemes;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Multipage reports consume the same immutable schedules used by the screen. */
public final class EmiReportRenderer {
    private EmiReportRenderer() { }
    public static void csv(Context c, OutputStream output, EmiResult a, EmiResult b, boolean yearly) throws IOException {
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        csvRow(writer, java.util.Arrays.asList(c.getString(R.string.emi_compare), c.getString(R.string.emi_csv_number), c.getString(R.string.emi_csv_date), c.getString(R.string.emi_csv_end_date), c.getString(R.string.emi_csv_periods),
                c.getString(R.string.emi_csv_currency), c.getString(R.string.emi_opening), c.getString(R.string.emi_payment),
                c.getString(R.string.emi_principal), c.getString(R.string.emi_interest_paid), c.getString(R.string.emi_extra),
                c.getString(R.string.emi_unused), c.getString(R.string.emi_closing)));
        EmiFormatter f = new EmiFormatter(c);
        for (int scenario = 0; scenario < (b == null ? 1 : 2); scenario++) {
            EmiResult r = scenario == 0 ? a : b; int index = 0;
            for (var row : EmiTableRows.build(r, yearly)) {
                csvRow(writer, java.util.Arrays.asList(c.getString(scenario == 0 ? R.string.emi_loan_a : R.string.emi_loan_b), Integer.toString(++index),
                        row.first().toString(), row.last().toString(), Integer.toString(row.count()), f.symbol(r.inputs().currencyCode()), row.opening().toPlainString(), row.payment().toPlainString(),
                        row.principal().toPlainString(), row.interest().toPlainString(), row.extra().toPlainString(), row.unused().toPlainString(), row.closing().toPlainString()));
            }
        }
        writer.flush();
    }
    private static void csvRow(java.io.Writer out, List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) { if (i > 0) out.write(','); out.write('"'); out.write(fields.get(i).replace("\"", "\"\"")); out.write('"'); } out.write("\r\n");
    }
    public static void pdf(Context c, OutputStream output, EmiResult a, EmiResult b, boolean yearly) throws IOException {
        android.content.res.Configuration paper = new android.content.res.Configuration(c.getResources().getConfiguration());
        paper.densityDpi = 160; paper.fontScale = 1f;
        c = c.createConfigurationContext(paper);
        PdfDocument document = new PdfDocument();
        Pages pages = new Pages(c, document);
        try {
            EmiFormatter f = new EmiFormatter(c);
            if (b != null) {
                pages.text(c.getString(R.string.emi_compare), pages.title);
                pages.text(c.getString(R.string.emi_loan_a) + "\n" + f.summary(a), pages.body);
                pages.text(c.getString(R.string.emi_loan_b) + "\n" + f.summary(b), pages.body);
                pages.text(c.getString(R.string.emi_difference) + "\n" + c.getString(R.string.emi_total) + ": " + f.money(b.totalPayment().subtract(a.totalPayment()), a), pages.body);
                pages.chart(EmiCharts.balance(c, a, b));
            }
            for (int scenario = 0; scenario < (b == null ? 1 : 2); scenario++) {
                EmiResult r = scenario == 0 ? a : b;
                if (scenario > 0 || b != null) pages.newPortraitPage();
                pages.text(c.getString(R.string.emi_report_title), pages.title);
                pages.text(c.getString(scenario == 0 ? R.string.emi_loan_a : R.string.emi_loan_b), pages.body);
                pages.text(f.summary(r), pages.body);
                pages.text(c.getString(R.string.emi_tenure) + ": " + c.getString(R.string.emi_payments_value, r.inputs().months()) + "\n"
                        + c.getString(R.string.emi_first_date) + ": " + f.date(r.inputs().firstEmi()), pages.body);
                if (!r.inputs().prepayments().isEmpty()) {
                    pages.text(c.getString(R.string.emi_prepayments) + " · " + c.getString(r.inputs().method() == EmiInputs.Method.REDUCE_TENURE ? R.string.emi_reduce_tenure : R.string.emi_reduce_emi), pages.body);
                    for (PrepaymentRule rule : r.inputs().prepayments()) pages.text(c.getString(R.string.emi_rule_label, f.money(rule.amount(), r),
                            c.getString(rule.interval() == 0 ? R.string.emi_once : rule.interval() == 1 ? R.string.emi_every_month : R.string.emi_every_year), rule.firstMonth(), rule.lastMonth()), pages.body);
                    pages.text(c.getString(R.string.emi_unused) + ": " + f.money(r.unusedPrepayment(), r) + "\n" + c.getString(R.string.emi_rule_after_payoff), pages.body);
                }
                pages.chart(EmiCharts.breakdown(c, r)); pages.chart(EmiCharts.balance(c, r, null));
                pages.table(r, yearly);
            }
            pages.finish(); document.writeTo(output);
        } finally { pages.finish(); document.close(); }
    }
    private static final class Pages {
        final Context c; final PdfDocument document; final int paperWidth, paperHeight, margin, body, title;
        int width, height;
        final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        PdfDocument.Page page; Canvas canvas; int pageNumber; float y;
        Pages(Context c, PdfDocument d) {
            this.c = c; document = d;
            width = c.getResources().getInteger(R.integer.emi_report_page_width); height = c.getResources().getInteger(R.integer.emi_report_page_height);
            paperWidth = width; paperHeight = height;
            margin = c.getResources().getInteger(R.integer.emi_report_margin); body = c.getResources().getInteger(R.integer.emi_report_body_size);
            title = c.getResources().getInteger(R.integer.emi_report_title_size); paint.setTypeface(AppearanceTypography.typeface(c));
            paint.setColor(CalculatorThemes.PORCELAIN_WHITE.primaryContentColor()); newPage();
        }
        StaticLayout layout(String text, int size, int available) {
            TextPaint p = new TextPaint(paint); p.setTextSize(size);
            return StaticLayout.Builder.obtain(text, 0, text.length(), p, Math.max(1, available)).setIncludePad(false).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
        }
        void newPage() { finish(); page = document.startPage(new PdfDocument.PageInfo.Builder(width, height, ++pageNumber).create()); canvas = page.getCanvas(); y = margin; }
        void newPortraitPage() { finish(); width = paperWidth; height = paperHeight; newPage(); }
        void landscapeTable() { if (width == paperWidth) { finish(); width = paperHeight; height = paperWidth; newPage(); } }
        void finish() {
            if (page == null) return;
            paint.setTextSize(body); canvas.drawText(c.getString(R.string.emi_report_page, pageNumber), margin, height - margin / 2f, paint);
            document.finishPage(page); page = null;
        }
        void text(String text, int size) throws IOException {
            StaticLayout layout = layout(text, size, width - margin * 2);
            if (layout.getHeight() > height - margin * 2) throw new IOException("Report paragraph exceeds page");
            if (y + layout.getHeight() > height - margin) newPage();
            canvas.save(); canvas.translate(margin, y); layout.draw(canvas); canvas.restore(); y += layout.getHeight() + body;
        }
        void chart(net.smartlogic.unitconverter.graphy.chart.ValueChartData data) {
            float chartHeight = body * 16f;
            if (y + chartHeight > height - margin) newPage();
            ValueChartRenderer.draw(c, canvas, new RectF(margin, y, width - margin, y + chartHeight), data, CalculatorThemes.PORCELAIN_WHITE, body);
            y += chartHeight + body;
        }
        void table(EmiResult r, boolean yearly) throws IOException {
            landscapeTable();
            EmiFormatter f = new EmiFormatter(c);
            List<String> headers = java.util.Arrays.asList(c.getString(R.string.emi_period), c.getString(R.string.emi_opening), c.getString(R.string.emi_payment),
                    c.getString(R.string.emi_principal), c.getString(R.string.emi_interest_paid), c.getString(R.string.emi_extra), c.getString(R.string.emi_closing), c.getString(R.string.emi_unused));
            text(c.getString(yearly ? R.string.emi_yearly_table : R.string.emi_monthly_table) + " · " + c.getString(R.string.emi_schedule), title);
            if (y + rowHeight(headers) + c.getResources().getInteger(R.integer.emi_report_row_height) > height - margin) newPage();
            tableRow(headers, true);
            for (var r0 : EmiTableRows.build(r, yearly)) {
                List<String> cells = java.util.Arrays.asList(r0.label(c, yearly), f.money(r0.opening(), r), f.money(r0.payment(), r), f.money(r0.principal(), r),
                        f.money(r0.interest(), r), f.money(r0.extra(), r), f.money(r0.closing(), r), f.money(r0.unused(), r));
                if (y + rowHeight(cells) > height - margin) { newPage(); text(c.getString(R.string.emi_schedule), title); tableRow(headers, true); }
                tableRow(cells, false);
            }
        }
        float rowHeight(List<String> cells) {
            int column = (width - margin * 2) / cells.size(); float h = c.getResources().getInteger(R.integer.emi_report_row_height);
            for (String cell : cells) h = Math.max(h, layout(cell, body, column - body / 2).getHeight() + body);
            return h;
        }
        void tableRow(List<String> cells, boolean header) {
            float h = rowHeight(cells);
            if (y + h > height - margin) newPage();
            int column = (width - margin * 2) / cells.size();
            for (int i = 0; i < cells.size(); i++) {
                canvas.save(); canvas.translate(margin + i * column, y); layout(cells.get(i), body, column - body / 2).draw(canvas); canvas.restore();
            }
            y += h;
            if (header) canvas.drawLine(margin, y - body / 2f, width - margin, y - body / 2f, paint);
        }
    }
}
