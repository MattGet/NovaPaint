package com.example.paint;

import javafx.scene.input.MouseEvent;

public class EllipseTool implements Tool {
    private double sx, sy, ex, ey;
    private boolean dragging;

    @Override public String getName(){ return "Ellipse"; }

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
        double x = Math.min(sx, ex), y = Math.min(sy, ey);
        double w = Math.abs(ex - sx), hgt = Math.abs(ey - sy);

        var g = s.getOverlay().getGraphicsContext2D();
        s.clearOverlay();
        g.setLineWidth(s.getBrush());
        g.setStroke(s.getStroke());
        g.setFill(s.getFill());
        if (s.getFill().getOpacity() > 0) g.fillOval(x, y, w, hgt);
        g.strokeOval(x, y, w, hgt);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;
        dragging = false;
        s.commitOverlay();
        h.push();
    }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        dragging = false;
        s.clearOverlay();
    }
}
