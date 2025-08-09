package com.example.paint;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class LineTool implements Tool {
    private double sx, sy, ex, ey;
    private boolean dragging;

    @Override public String getName(){ return "Line"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        dragging = true;
        sx = ex = e.getX();
        sy = ey = e.getY();
        s.clearOverlay();
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;
        ex = e.getX(); ey = e.getY();
        var g = s.getOverlay().getGraphicsContext2D();
        s.clearOverlay();
        g.setStroke(s.getStroke());
        g.setLineWidth(s.getBrush());
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.strokeLine(sx, sy, ex, ey);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;
        dragging = false;
        // bake preview to base, then save one history step
        s.commitOverlay();
        h.push();
    }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        dragging = false;
        s.clearOverlay();
    }
}
