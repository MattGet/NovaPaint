package com.example.paint;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * Polygon tool:
 *  - Click to set the first vertex.
 *  - Click-drag-release: commits ONE segment (last → release point) and pushes history.
 *  - While dragging, shows a preview line (snaps to first vertex when near).
 *  - Double-click: closes shape to first vertex and finishes.
 *  - Esc (onCancel): finishes without closing (segments already drawn remain).
 */
public class PolygonTool implements Tool {

    private boolean active = false;
    private boolean dragging = false;

    private double firstX, firstY;   // first vertex
    private double lastX, lastY;     // last committed vertex

    private static final double SNAP_RADIUS_PX = 10.0;

    @Override public String getName() { return "Polygon"; }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        s.setStatus("Polygon: click-drag-release to add segments, double-click to close, Esc to finish.");
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        // Double-click closes polygon back to first vertex
        if (active && e.getClickCount() == 2) {
            commitSegment(s, h, lastX, lastY, firstX, firstY);
            finish(s);
            return;
        }

        if (!active) {
            // Start a new polygon
            active = true;
            dragging = true;
            firstX = lastX = e.getX();
            firstY = lastY = e.getY();
            s.clearOverlay();
            drawFirstVertexMarker(s);
        } else {
            // Continue polygon: start new drag from last committed vertex
            dragging = true;
            s.clearOverlay();
            drawFirstVertexMarker(s);
        }
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!active || !dragging) return;

        // Live preview from last vertex to current mouse (snap to first if close)
        Point2D snap = maybeSnapToFirst(e.getX(), e.getY());
        double tx = (snap != null) ? snap.getX() : e.getX();
        double ty = (snap != null) ? snap.getY() : e.getY();

        var g = s.getOverlay().getGraphicsContext2D();
        s.clearOverlay();

        g.setLineWidth(Math.max(1, s.getBrush()));
        g.setStroke(s.getStroke());
        g.setLineDashes(0);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.strokeLine(lastX, lastY, tx, ty);

        // keep first-vertex marker visible for snapping hint
        drawFirstVertexMarker(s);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!active || !dragging) return;
        dragging = false;

        // Finalize and commit ONE segment
        Point2D snap = maybeSnapToFirst(e.getX(), e.getY());
        double tx = (snap != null) ? snap.getX() : e.getX();
        double ty = (snap != null) ? snap.getY() : e.getY();

        // Ignore tiny zero-length segments
        if (Math.hypot(tx - lastX, ty - lastY) < 0.5) {
            s.clearOverlay();
            drawFirstVertexMarker(s);
            return;
        }

        commitSegment(s, h, lastX, lastY, tx, ty);

        // Update last vertex to the new committed endpoint
        lastX = tx; lastY = ty;

        // After commit, keep only the first-vertex marker visible (no line preview until next drag)
        s.clearOverlay();
        drawFirstVertexMarker(s);
    }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        finish(s);
    }

    // --- helpers ---

    private void commitSegment(CanvasState s, HistoryManager h, double x1, double y1, double x2, double y2) {
        GraphicsContext gb = s.getBase().getGraphicsContext2D();
        gb.setLineWidth(Math.max(1, s.getBrush()));
        gb.setStroke(s.getStroke());
        gb.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gb.strokeLine(x1, y1, x2, y2);

        // One history entry per segment
        h.push();
    }

    private void drawFirstVertexMarker(CanvasState s) {
        var g = s.getOverlay().getGraphicsContext2D();
        double r = 3.0;
        g.setFill(Color.color(1, 1, 1, 0.9));
        g.fillOval(firstX - r, firstY - r, r * 2, r * 2);
        g.setStroke(Color.color(0, 0, 0, 0.85));
        g.setLineWidth(1.2);
        g.strokeOval(firstX - r, firstY - r, r * 2, r * 2);
    }

    private Point2D maybeSnapToFirst(double mx, double my) {
        if (!active) return null;
        return (Math.hypot(mx - firstX, my - firstY) <= SNAP_RADIUS_PX)
                ? new Point2D(firstX, firstY) : null;
    }

    private void finish(CanvasState s) {
        active = false;
        dragging = false;
        s.clearOverlay();
        s.setStatus("Polygon finished");
    }
}
