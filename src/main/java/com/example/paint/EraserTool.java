package com.example.paint;

import javafx.beans.value.ChangeListener;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class EraserTool implements Tool {
    private final List<double[]> points = new ArrayList<>();
    private Cursor currentCursor = Cursor.DEFAULT;
    private ChangeListener<Number> brushListener;
    private boolean drew = false;

    private static final double STAMP_STEP = 0.6;

    @Override public String getName(){ return "Eraser"; }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        installCursor(s);
        brushListener = (obs, o, v) -> installCursor(s);
        s.getBrushSlider().valueProperty().addListener(brushListener);
    }

    @Override
    public void onDeselect(CanvasState s, HistoryManager h) {
        try {
            if (brushListener != null) s.getBrushSlider().valueProperty().removeListener(brushListener);
        } catch (Exception ignored) {}
        s.getOverlay().setCursor(Cursor.DEFAULT);
    }

    private void installCursor(CanvasState s) {
        double diameter = Math.max(2, s.getBrush() * s.getZoom());
        Color ring = Color.WHITE;
        currentCursor = CursorFactory.brushRing(ring, diameter, Math.max(1.5, Math.min(3.5, diameter * 0.09)));
        s.getOverlay().setCursor(currentCursor);
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        points.clear();
        drew = false;
        points.add(new double[]{e.getX(), e.getY()});
        stampCircle(s.getBase().getGraphicsContext2D(), e.getX(), e.getY(), s.getBrush());
        drew = true;
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        points.add(new double[]{e.getX(), e.getY()});
        GraphicsContext g = s.getBase().getGraphicsContext2D();
        double brush = s.getBrush();

        if (points.size() < 3) {
            double[] p = points.get(points.size()-2);
            stampSegment(g, p[0], p[1], e.getX(), e.getY(), brush);
            drew = true;
            return;
        }
        int last = points.size() - 1;
        double[] p0 = points.get(last - 2);
        double[] p1 = points.get(last - 1);
        double[] p2 = points.get(last);

        double mx1 = (p0[0] + p1[0]) / 2.0;
        double my1 = (p0[1] + p1[1]) / 2.0;
        double mx2 = (p1[0] + p2[0]) / 2.0;
        double my2 = (p1[1] + p2[1]) / 2.0;

        stampQuadratic(g, mx1, my1, p1[0], p1[1], mx2, my2, brush);
        drew = true;
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (drew) h.push(); // one snapshot per stroke at finish
        points.clear();
        drew = false;
    }

    // ---- stamping helpers (unchanged) ----
    private void stampCircle(GraphicsContext g, double x, double y, double diameter) {
        PixelWriter pw = g.getPixelWriter();
        if (pw == null) return;
        double r = diameter * 0.5;
        int minX = (int)Math.floor(x - r), minY = (int)Math.floor(y - r);
        int maxX = (int)Math.ceil (x + r), maxY = (int)Math.ceil (y + r);
        int width  = (int) g.getCanvas().getWidth();
        int height = (int) g.getCanvas().getHeight();
        minX = Math.max(0, Math.min(minX, width  - 1));
        maxX = Math.max(0, Math.min(maxX, width  - 1));
        minY = Math.max(0, Math.min(minY, height - 1));
        maxY = Math.max(0, Math.min(maxY, height - 1));
        double r2 = r * r;

        for (int yy = minY; yy <= maxY; yy++) {
            double dy = (yy + 0.5) - y;
            double dy2 = dy * dy;
            double span = Math.sqrt(Math.max(0.0, r2 - dy2));
            int sx = (int)Math.floor(x - span);
            int ex = (int)Math.ceil (x + span);
            sx = Math.max(sx, minX);
            ex = Math.min(ex, maxX);
            for (int xx = sx; xx <= ex; xx++) {
                pw.setArgb(xx, yy, 0x00000000);
            }
        }
    }

    private void stampSegment(GraphicsContext g, double x1, double y1, double x2, double y2, double diameter) {
        double dx = x2 - x1, dy = y2 - y1;
        double dist = Math.hypot(dx, dy);
        if (dist == 0) { stampCircle(g, x1, y1, diameter); return; }
        double step = Math.max(STAMP_STEP, diameter * 0.20);
        int n = Math.max(1, (int)(dist / step));
        double sx = dx / n, sy = dy / n;
        double x = x1, y = y1;
        for (int i = 0; i <= n; i++) {
            stampCircle(g, x, y, diameter);
            x += sx; y += sy;
        }
    }

    private void stampQuadratic(GraphicsContext g, double x0, double y0, double cx, double cy, double x2, double y2, double diameter) {
        double len = approxQuadLength(x0,y0,cx,cy,x2,y2);
        double step = Math.max(STAMP_STEP, diameter * 0.20);
        int n = Math.max(2, (int)(len / step));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double x = quad(x0, cx, x2, t);
            double y = quad(y0, cy, y2, t);
            stampCircle(g, x, y, diameter);
        }
    }

    private static double quad(double p0, double c, double p1, double t) {
        double u = 1 - t;
        return u*u*p0 + 2*u*t*c + t*t*p1;
    }

    private static double approxQuadLength(double x0,double y0,double cx,double cy,double x1,double y1) {
        double d1 = Math.hypot(cx - x0, cy - y0);
        double d2 = Math.hypot(x1 - cx, y1 - cy);
        double chord = Math.hypot(x1 - x0, y1 - y0);
        return (d1 + d2 + chord) / 2.0;
    }
}
