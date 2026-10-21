package net.smartlogic.unitconverter.graphy.renderer;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.model.*;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/** Complete, fully expanded, white-paper exports using the interactive graph's primitives. */
public final class GraphyDocumentRenderer {
    public enum Format {
        PDF("application/pdf", "pdf"), PNG("image/png", "png");
        public final String mimeType, extension;
        Format(String mimeType, String extension) { this.mimeType = mimeType; this.extension = extension; }
    }

    private GraphyDocumentRenderer() { }

    public static Document prepare(Context context, GraphyOutput output, int viewportWidth) throws IOException {
        if (output == null || GraphyTopology.orderedNodes(output).isEmpty()) throw new IOException("Empty graph");
        GraphyViewTheme theme = GraphyViewTheme.forExport(context);
        var graph = new GraphLayoutEngine().layout(output, theme, Math.max(1, viewportWidth), Collections.emptySet(), false);
        return new Document(theme, graph, output);
    }

    /** A thread-confined, measured document; header/graph/footer regions never intersect. */
    public static final class Document {
        final GraphLayoutEngine.LayoutResult graph;
        final RectF headerBounds, graphBounds, legendBounds;
        final List<Watermark> watermarks = new ArrayList<>();
        final List<LegendPlacement> legendItems = new ArrayList<>();
        final int legendRowCount;
        public final int width, height;
        private final GraphyViewTheme theme;
        private final GraphyCanvasRenderer renderer;
        private final StaticLayout expression, result, notice;
        private final Paint watermarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String brand;
        private final float margin, gap, rowGap, symbol;

        private Document(GraphyViewTheme theme, GraphLayoutEngine.LayoutResult graph, GraphyOutput output) throws IOException {
            this.theme = theme;
            this.graph = graph;
            renderer = new GraphyCanvasRenderer(graph, theme);
            var res = theme.getResources();
            margin = res.getDimension(R.dimen.graphy_export_margin);
            gap = res.getDimension(R.dimen.graphy_export_gap);
            rowGap = res.getDimension(R.dimen.graphy_spacing_sm);
            symbol = res.getDimension(R.dimen.graphy_export_legend_symbol);
            Set<GraphyNodeType> present = EnumSet.noneOf(GraphyNodeType.class);
            for (GraphyNode node : output.getNodes()) present.add(node.type());
            TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            labelPaint.setTypeface(theme.getTypeface()); labelPaint.setTextSize(theme.getLabelTextSize());
            List<LegendEntry> entries = new ArrayList<>();
            for (GraphyNodeType type : GraphyNodeType.values()) if (present.contains(type)) {
                String label = theme.nodeRole(type);
                float labelWidth = Math.max(1, (float) Math.ceil(labelPaint.measureText(label)));
                StaticLayout layout = text(label, theme.getLabelTextSize(), false, labelWidth, Layout.Alignment.ALIGN_NORMAL, 255);
                entries.add(new LegendEntry(type, layout, symbol + rowGap + layout.getWidth(), Math.max(symbol, layout.getHeight())));
            }
            int split = entries.size();
            float smallestTwoRows = legendWidth(entries);
            for (int i = 1; i < entries.size(); i++) {
                float needed = Math.max(legendWidth(entries.subList(0,i)), legendWidth(entries.subList(i,entries.size())));
                if (needed < smallestTwoRows) { smallestTwoRows = needed; split = i; }
            }
            float inner = Math.max(graph.contentWidth(), smallestTwoRows);
            width = (int) Math.ceil(inner + margin * 2);
            inner = width - margin * 2;
            List<List<LegendEntry>> rows = new ArrayList<>();
            if (legendWidth(entries) <= inner) rows.add(entries);
            else { rows.add(entries.subList(0, split)); rows.add(entries.subList(split, entries.size())); }
            legendRowCount = rows.size();
            expression = text(output.getMetadata().get(GraphyOutput.METADATA_CONTEXT) instanceof String context
                    ? context : output.getExpression(), res.getDimension(R.dimen.graphy_export_title_text), false, inner, Layout.Alignment.ALIGN_CENTER, 255);
            result = text(res.getString(R.string.graphy_export_result, output.getResult()),
                    res.getDimension(R.dimen.graphy_export_result_text), true, inner, Layout.Alignment.ALIGN_CENTER, 255);
            notice = text(output.getMetadata().get(GraphyOutput.METADATA_NOTICE) instanceof String context ? context : "",
                    theme.getLabelTextSize(), false, inner, Layout.Alignment.ALIGN_CENTER, 255);
            float headerHeight = expression.getHeight() + rowGap + result.getHeight();
            if (notice.getText().length() > 0) headerHeight += rowGap + notice.getHeight();
            headerBounds = new RectF(margin, margin, width - margin, margin + headerHeight);
            float graphX = margin + (inner - graph.contentWidth()) / 2;
            graphBounds = new RectF(graphX, headerBounds.bottom + gap, graphX + graph.contentWidth(),
                    headerBounds.bottom + gap + graph.contentHeight());
            float footerTop = graphBounds.bottom + gap;
            float y = footerTop + rowGap;
            for (List<LegendEntry> row : rows) {
                float rowHeight = 0;
                for (LegendEntry entry : row) rowHeight = Math.max(rowHeight, entry.height());
                float x = width - margin - legendWidth(row);
                for (LegendEntry entry : row) {
                    legendItems.add(new LegendPlacement(entry, new RectF(x, y, x + entry.width(), y + rowHeight)));
                    x += entry.width() + theme.getHorizontalGap();
                }
                y += rowHeight + rowGap;
            }
            legendBounds = new RectF(margin, footerTop, width - margin, y);
            height = (int) Math.ceil(legendBounds.bottom + margin);
            if (width <= 0 || height <= 0) throw new IOException("Invalid document dimensions");
            brand = res.getString(R.string.app_name);
            watermarkPaint.setTypeface(theme.getTypeface());
            watermarkPaint.setTextSize(res.getDimension(R.dimen.graphy_export_watermark_text));
            watermarkPaint.setColor(theme.getPrimaryTextColor());
            watermarkPaint.setAlpha(res.getInteger(R.integer.graphy_export_watermark_alpha));
            layoutWatermarks();
        }

        private float legendWidth(List<LegendEntry> entries) {
            float value = 0;
            for (LegendEntry entry : entries) value += entry.width();
            return value + Math.max(0, entries.size() - 1) * theme.getHorizontalGap();
        }

        private void layoutWatermarks() {
            // One large diagonal mark for ordinary documents; repeat on very tall/wide
            // pages so the background marking still spans at least 40% of the document.
            Rect ink = new Rect(); watermarkPaint.getTextBounds(brand, 0, brand.length(), ink);
            float areaWidth = width - margin * 2, areaHeight = graphBounds.bottom - margin;
            int columns = Math.max(1, Math.round(areaWidth / areaHeight));
            int rows = Math.max(1, Math.round(areaHeight / areaWidth));
            float cellWidth = areaWidth / columns, cellHeight = areaHeight / rows;
            float inset = theme.getNodePadding();
            for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
                float targetWidth = cellWidth - inset * 2, targetHeight = cellHeight - inset * 2;
                double ratio = targetHeight / targetWidth;
                double tangent = (ratio * ink.width() - ink.height()) / (ink.width() - ratio * ink.height());
                double radians = Math.atan(Math.max(0, tangent));
                float rotatedWidth = (float) (ink.width() * Math.cos(radians) + ink.height() * Math.sin(radians));
                float rotatedHeight = (float) (ink.width() * Math.sin(radians) + ink.height() * Math.cos(radians));
                float scale = Math.min(targetWidth / rotatedWidth, targetHeight / rotatedHeight);
                float cx = margin + cellWidth * (column + .5f), cy = margin + cellHeight * (row + .5f);
                RectF bounds = new RectF(cx - rotatedWidth * scale / 2, cy - rotatedHeight * scale / 2,
                        cx + rotatedWidth * scale / 2, cy + rotatedHeight * scale / 2);
                watermarks.add(new Watermark(bounds, cx, cy, (float) -Math.toDegrees(radians), scale,
                        -ink.exactCenterX(), -ink.exactCenterY()));
            }
        }

        void drawWatermarks(Canvas canvas) {
            for (Watermark mark : watermarks) {
                canvas.save(); canvas.translate(mark.cx(), mark.cy()); canvas.rotate(mark.degrees()); canvas.scale(mark.scale(), mark.scale());
                canvas.drawText(brand, mark.textX(), mark.textY(), watermarkPaint); canvas.restore();
            }
        }

        private StaticLayout text(String content, float size, boolean bold, float width, Layout.Alignment alignment, int alpha) {
            TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            paint.setTypeface(Typeface.create(theme.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextSize(size);
            paint.setColor(theme.getPrimaryTextColor());
            paint.setAlpha(alpha);
            return StaticLayout.Builder.obtain(content, 0, content.length(), paint, Math.max(1, (int) width))
                    .setAlignment(alignment).setIncludePad(false).build();
        }

        private void drawText(Canvas canvas, StaticLayout layout, float x, float y) {
            canvas.save(); canvas.translate(x, y); layout.draw(canvas); canvas.restore();
        }

        public void draw(Canvas canvas) {
            canvas.drawColor(theme.getBackgroundColor());
            drawWatermarks(canvas);
            float y = headerBounds.top;
            drawText(canvas, expression, headerBounds.left, y); y += expression.getHeight() + rowGap;
            drawText(canvas, result, headerBounds.left, y); y += result.getHeight() + rowGap;
            if (notice.getText().length() > 0) drawText(canvas, notice, headerBounds.left, y);
            canvas.save(); canvas.translate(graphBounds.left, graphBounds.top); renderer.draw(canvas); canvas.restore();
            Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG);
            rule.setColor(theme.getConnectorColor()); rule.setStrokeWidth(theme.getNodeBorderStroke());
            canvas.drawLine(legendBounds.left, legendBounds.top, legendBounds.right, legendBounds.top, rule);
            for (LegendPlacement placement : legendItems) {
                LegendEntry entry = placement.entry(); RectF bounds = placement.bounds();
                float top = bounds.centerY() - symbol / 2;
                renderer.drawNodeFrame(canvas, new RectF(bounds.left, top, bounds.left + symbol, top + symbol), entry.type());
                drawText(canvas, entry.label(), bounds.left + symbol + rowGap, bounds.centerY() - entry.label().getHeight() / 2f);
            }
        }

        public void write(OutputStream destination, Format format) throws IOException {
            if (format == Format.PDF) {
                PdfDocument pdf = new PdfDocument();
                try {
                    PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(width, height, 1).create());
                    try { draw(page.getCanvas()); }
                    finally { pdf.finishPage(page); }
                    pdf.writeTo(destination);
                } finally { pdf.close(); }
            } else {
                float scale = theme.getResources().getInteger(R.integer.graphy_export_png_scale);
                int maxEdge = theme.getResources().getInteger(R.integer.graphy_export_png_max_edge);
                int maxPixels = theme.getResources().getInteger(R.integer.graphy_export_png_max_pixels);
                scale = Math.min(scale, Math.min((float) maxEdge / Math.max(width, height),
                        (float) Math.sqrt((double) maxPixels / ((double) width * height))));
                if (scale < 1f) throw new IOException(theme.getResources().getString(R.string.graphy_export_png_too_large));
                Bitmap bitmap;
                try { bitmap = Bitmap.createBitmap((int) (width * scale), (int) (height * scale), Bitmap.Config.ARGB_8888); }
                catch (OutOfMemoryError allocationFailure) {
                    throw new IOException(theme.getResources().getString(R.string.graphy_export_png_too_large), allocationFailure);
                }
                try {
                    Canvas canvas = new Canvas(bitmap); canvas.scale(scale, scale); draw(canvas);
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, destination)) throw new IOException("PNG encoding failed");
                } finally { bitmap.recycle(); }
            }
        }
        record LegendEntry(GraphyNodeType type, StaticLayout label, float width, float height) { }
        record LegendPlacement(LegendEntry entry, RectF bounds) { }
        record Watermark(RectF bounds, float cx, float cy, float degrees, float scale, float textX, float textY) { }
    }
}
