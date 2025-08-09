package com.example.paint;

import javafx.scene.input.MouseEvent;

public class RectTool implements Tool {
    private double sx, sy;

    @Override public String getName(){ return "Rectangle"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        h.push();
        sx = e.getX(); sy = e.getY();
        s.clearOverlay();
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        var g = s.getOverlay().getGraphicsContext2D();
        s.clearOverlay();
        double x = Math.min(sx, e.getX());
        double y = Math.min(sy, e.getY());
        double width = Math.abs(e.getX() - sx);
        double height = Math.abs(e.getY() - sy);
        g.setLineWidth(s.getBrush());
        g.setStroke(s.getStroke());
        if (s.getFill().getOpacity() > 0) {
            g.setFill(s.getFill());
            g.fillRect(x, y, width, height);
        }
        g.strokeRect(x, y, width, height);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        s.commitOverlay();
    }
}
