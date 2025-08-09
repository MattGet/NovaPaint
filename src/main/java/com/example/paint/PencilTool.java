package com.example.paint;

import javafx.beans.value.ChangeListener;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class PencilTool implements Tool {
    private final List<double[]> points = new ArrayList<>();
    private Cursor currentCursor = Cursor.DEFAULT;
    private ChangeListener<Number> brushListener;
    private ChangeListener<Color> colorListener;
    private boolean drew = false; // track if anything was actually drawn

    @Override public String getName(){ return "Pencil"; }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        installCursor(s);
        brushListener = (obs, o, v) -> installCursor(s);
        colorListener = (obs, o, v) -> installCursor(s);
        s.getBrushSlider().valueProperty().addListener(brushListener);
        s.getStrokePicker().valueProperty().addListener(colorListener);
    }

    @Override
    public void onDeselect(CanvasState s, HistoryManager h) {
        try {
            if (brushListener != null) s.getBrushSlider().valueProperty().removeListener(brushListener);
            if (colorListener != null) s.getStrokePicker().valueProperty().removeListener(colorListener);
        } catch (Exception ignored) {}
        s.getOverlay().setCursor(Cursor.DEFAULT);
    }

    private void installCursor(CanvasState s) {
        double diameter = Math.max(2, s.getBrush() * s.getZoom());
        Color ring = s.getStroke();
        currentCursor = CursorFactory.brushRing(ring, diameter, Math.max(1.2, Math.min(3.0, diameter * 0.08)));
        s.getOverlay().setCursor(currentCursor);
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        points.clear();
        drew = false;

        points.add(new double[]{e.getX(), e.getY()});
        GraphicsContext g = s.getBase().getGraphicsContext2D();
        g.setStroke(s.getStroke());
        g.setLineWidth(s.getBrush());
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(e.getX(), e.getY());
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        points.add(new double[]{e.getX(), e.getY()});
        GraphicsContext g = s.getBase().getGraphicsContext2D();
        g.setStroke(s.getStroke());
        g.setLineWidth(s.getBrush());
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        if (points.size() < 3) {
            g.lineTo(e.getX(), e.getY());
            g.stroke();
            drew = true;
            return;
        }
        int last = points.size() - 1;
        double[] p0 = points.get(last - 2);
        double[] p1 = points.get(last - 1);
        double[] p2 = points.get(last);

        double midX1 = (p0[0] + p1[0]) / 2.0;
        double midY1 = (p0[1] + p1[1]) / 2.0;
        double midX2 = (p1[0] + p2[0]) / 2.0;
        double midY2 = (p1[1] + p2[1]) / 2.0;

        g.beginPath();
        g.moveTo(midX1, midY1);
        g.quadraticCurveTo(p1[0], p1[1], midX2, midY2);
        g.stroke();
        drew = true;
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (drew) h.push();   // save exactly once per stroke, after finishing
        points.clear();
        drew = false;
    }
}
