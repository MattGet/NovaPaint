package com.example.paint;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class PolygonTool implements Tool, PolygonCapable, Hoverable {
    // Snap radius in screen pixels (scene coords) for intuitive behavior under zoom
    private static final double SNAP_RADIUS_PX = 10.0;

    private final List<Double> xs = new ArrayList<>();
    private final List<Double> ys = new ArrayList<>();
    private boolean active = false;

    // current preview target (in BASE/local coords)
    private double hoverX, hoverY;
    private boolean hasHover = false;

    // snapping state
    private int snappedIndex = -1;           // -1 = none, otherwise index of snapped vertex

    @Override public String getName(){ return "Polygon"; }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        s.clearOverlay();
        s.setStatus("Polygon: click to add points, Enter to commit, Esc to cancel. Snap near existing vertices.");
        resetHover();
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        // Convert click to base-local
        Point2D basePt = s.getBase().sceneToLocal(e.getSceneX(), e.getSceneY());
        double cx = basePt.getX(), cy = basePt.getY();

        // Activate on first point (and push a single undo step for the whole polygon)
        if (!active) {
            h.push();
            xs.clear(); ys.clear();
            active = true;
        }

        // Apply snapping on click as well
        int near = findSnapVertexScene(s, e.getSceneX(), e.getSceneY());
        if (near >= 0) {
            // If snapping to first vertex and we have >=2 segments already -> close polygon
            if (near == 0 && xs.size() >= 2) {
                commitPolygon(s, h);
                return;
            }
            cx = xs.get(near);
            cy = ys.get(near);
        }

        xs.add(cx); ys.add(cy);
        // Update preview target to the clicked (snapped) location, so the next hover starts here
        hoverX = cx; hoverY = cy; hasHover = true;
        drawPreview(s);
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        // Treat drag like hover (live preview while button held)
        onMove(s, e.getSceneX(), e.getSceneY());
    }

    @Override public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) { /* no-op */ }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        clearState(s);
        s.setStatus("Polygon: canceled.");
    }

    @Override
    public void onDeselect(CanvasState s, HistoryManager h) {
        clearState(s);
    }

    // ===== Hoverable: live preview under the mouse =====
    @Override
    public void onMove(CanvasState s, double sceneX, double sceneY) {
        if (!active || xs.isEmpty()) {
            // still show points if any, otherwise just clear overlay
            drawPreview(s);
            return;
        }

        // Convert mouse to base-local for drawing
        Point2D basePt = s.getBase().sceneToLocal(sceneX, sceneY);
        double tx = basePt.getX(), ty = basePt.getY();

        // Check for snapping in scene space (zoom consistent)
        int near = findSnapVertexScene(s, sceneX, sceneY);
        if (near >= 0) {
            snappedIndex = near;
            tx = xs.get(near);
            ty = ys.get(near);
        } else {
            snappedIndex = -1;
        }

        hoverX = tx; hoverY = ty; hasHover = true;
        drawPreview(s);
    }

    // ===== Commit =====
    @Override
    public void commitPolygon(CanvasState s, HistoryManager h) {
        if (!active || xs.size() < 3) { clearState(s); return; }

        var g = s.getBase().getGraphicsContext2D();
        g.setLineWidth(s.getBrush());
        g.setStroke(s.getStroke());
        if (s.getFill().getOpacity() > 0) {
            g.setFill(s.getFill());
            g.fillPolygon(toArray(xs), toArray(ys), xs.size());
        }
        g.strokePolygon(toArray(xs), toArray(ys), xs.size());

        s.clearOverlay();
        clearState(s);
        // (We pushed to history when we started the polygon)
    }

    // ===== Drawing the preview =====
    private void drawPreview(CanvasState s) {
        var g = s.getOverlay().getGraphicsContext2D();
        s.clearOverlay();

        // Draw existing edges
        g.setLineWidth(s.getBrush());
        g.setStroke(s.getStroke());

        for (int i = 1; i < xs.size(); i++) {
            g.strokeLine(xs.get(i-1), ys.get(i-1), xs.get(i), ys.get(i));
        }

        // Rubber-band: last vertex -> current hover
        if (hasHover && !xs.isEmpty()) {
            double lx = xs.get(xs.size()-1);
            double ly = ys.get(ys.size()-1);
            g.setLineDashes(null);
            g.strokeLine(lx, ly, hoverX, hoverY);
        }

        // Draw small handles on vertices
        double r = Math.max(3, Math.min(6, s.getBrush())); // handle size
        g.setFill(Color.rgb(255,255,255,0.85));
        g.setStroke(Color.DARKSLATEGRAY);

        for (int i = 0; i < xs.size(); i++) {
            double vx = xs.get(i), vy = ys.get(i);
            g.fillOval(vx - r, vy - r, r*2, r*2);
            g.strokeOval(vx - r, vy - r, r*2, r*2);
        }

        // Highlight snapped vertex (bigger + colored ring)
        if (snappedIndex >= 0) {
            double vx = xs.get(snappedIndex), vy = ys.get(snappedIndex);
            double R = r + 3;
            g.setStroke(Color.ORANGE);
            g.setLineWidth(2.0);
            g.strokeOval(vx - R, vy - R, R*2, R*2);
        }
    }

    // ===== Snapping (in SCENE pixels) =====
    /** Returns index of vertex to snap to, or -1 if none within SNAP_RADIUS_PX. */
    private int findSnapVertexScene(CanvasState s, double sceneX, double sceneY) {
        if (xs.isEmpty()) return -1;
        int best = -1;
        double bestD2 = SNAP_RADIUS_PX * SNAP_RADIUS_PX;

        for (int i = 0; i < xs.size(); i++) {
            // Convert each vertex (base-local) to scene coords and compare in screen pixels
            Point2D vScene = s.getBase().localToScene(xs.get(i), ys.get(i));
            double dx = vScene.getX() - sceneX;
            double dy = vScene.getY() - sceneY;
            double d2 = dx*dx + dy*dy;
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = i;
            }
        }
        return best;
    }

    // ===== housekeeping =====
    private void clearState(CanvasState s) {
        active = false;
        xs.clear(); ys.clear();
        resetHover();
        s.clearOverlay();
    }

    private void resetHover() { hasHover = false; snappedIndex = -1; }

    private double[] toArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
