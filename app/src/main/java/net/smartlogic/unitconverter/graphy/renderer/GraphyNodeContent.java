package net.smartlogic.unitconverter.graphy.renderer;

import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import net.smartlogic.unitconverter.graphy.model.GraphyNode;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme;

/** Measures and draws the same wrapping text, without truncation or font shrinking. */
record GraphyNodeContent(StaticLayout caption, StaticLayout value, float width, float height,
                         float padding, boolean compact) {
    static GraphyNodeContent measure(GraphyNode node, GraphyViewTheme theme, float maxWidth) {
        float padding = theme.getNodePadding();
        boolean operation = node.type() == GraphyNodeType.OPERATION;
        TextPaint valuePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        valuePaint.setTypeface(Typeface.create(theme.getTypeface(), node.type() == GraphyNodeType.RESULT
                ? Typeface.BOLD : Typeface.NORMAL));
        valuePaint.setTextSize(node.type() == GraphyNodeType.RESULT ? theme.getResultTextSize()
                : operation ? theme.getOperationTextSize() : theme.getValueTextSize());
        valuePaint.setColor(theme.nodeContent(node.type()));
        String text = operation ? GraphLayoutEngine.formatOperator(node.displayValue()) : node.displayValue();
        boolean compact = operation && text.length() <= 3
                && valuePaint.measureText(text) + padding * 2 <= theme.getOperationMarkerSize();
        TextPaint captionPaint = new TextPaint(valuePaint);
        captionPaint.setTextSize(theme.getLabelTextSize());
        captionPaint.setTypeface(Typeface.create(theme.getTypeface(), Typeface.NORMAL));
        String label = compact ? "" : theme.nodeCaption(node);
        float natural = Math.max(valuePaint.measureText(text), captionPaint.measureText(label)) + padding * 2;
        float width = Math.min(maxWidth, compact ? theme.getOperationMarkerSize()
                : Math.max(theme.getNodeMinWidth(), natural));
        int textWidth = Math.max(1, (int) (width - padding * 2));
        StaticLayout caption = textLayout(label, captionPaint, textWidth);
        StaticLayout value = textLayout(text, valuePaint, textWidth);
        float textHeight = value.getHeight() + (compact ? 0 : caption.getHeight() + padding / 2);
        float height = Math.max(compact ? theme.getOperationMarkerSize() : theme.getNodeMinHeight(),
                textHeight + padding * 2);
        return new GraphyNodeContent(caption, value, width, height, padding, compact);
    }

    private static StaticLayout textLayout(String text, TextPaint paint, int width) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false)
                .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY).build();
    }

    void draw(android.graphics.Canvas canvas, android.graphics.RectF bounds) {
        float textHeight = value.getHeight() + (compact ? 0 : caption.getHeight() + padding / 2);
        canvas.save();
        canvas.translate(bounds.left + (bounds.width() - value.getWidth()) / 2,
                bounds.top + (bounds.height() - textHeight) / 2);
        if (!compact) {
            caption.draw(canvas);
            canvas.translate(0, caption.getHeight() + padding / 2);
        }
        value.draw(canvas);
        canvas.restore();
    }
}
