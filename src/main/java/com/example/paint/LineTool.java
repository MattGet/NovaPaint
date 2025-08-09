package com.example.paint;

import javafx.scene.input.MouseEvent;

public class LineTool implements Tool {
    protected double sx, sy;

    @Override public String getName(){ return "Line"; }

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
        g.setLineWidth(s.getBrush());
        g.setStroke(s.getStroke());
        g.strokeLine(sx, sy, e.getX(), e.getY());
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        s.commitOverlay();
    }
}
