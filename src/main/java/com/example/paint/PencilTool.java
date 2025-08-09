package com.example.paint;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class PencilTool implements Tool {
    private double lastX, lastY;
    private final boolean eraser;

    public PencilTool(boolean eraser){ this.eraser = eraser; }

    @Override public String getName(){ return eraser? "Eraser" : "Pencil"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        h.push();
        lastX = e.getX(); lastY = e.getY();
        var g = s.getBase().getGraphicsContext2D();
        g.setLineWidth(s.getBrush());
        g.setStroke(eraser ? Color.WHITE : s.getStroke());
        g.beginPath(); g.moveTo(lastX, lastY); g.stroke();
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        var g = s.getBase().getGraphicsContext2D();
        g.setLineWidth(s.getBrush());
        g.setStroke(eraser ? Color.WHITE : s.getStroke());
        g.lineTo(e.getX(), e.getY());
        g.stroke();
    }
}
