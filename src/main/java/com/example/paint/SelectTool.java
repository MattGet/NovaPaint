package com.example.paint;

import javafx.geometry.Point2D;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class SelectTool implements Tool {
    private double sx, sy;    // start in base-local coords
    private boolean dragging;
    private MoveTool moveTool;

    public void setMoveTool(MoveTool move) { this.moveTool = move; }

    @Override public String getName() { return "Select"; }

    @Override public void onSelect(CanvasState s, HistoryManager h) { s.clearOverlay(); }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        h.push();
        Point2D p = s.getBase().sceneToLocal(e.getSceneX(), e.getSceneY());
        sx = p.getX(); sy = p.getY();
        dragging = true;
        s.clearOverlay();
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;
        Point2D p = s.getBase().sceneToLocal(e.getSceneX(), e.getSceneY());
        double x = Math.min(sx, p.getX());
        double y = Math.min(sy, p.getY());
        double width  = Math.abs(p.getX() - sx);
        double height = Math.abs(p.getY() - sy);

        var g = s.getOverlay().getGraphicsContext2D();
        s.clearOverlay();
        g.setLineDashes(6);
        g.setStroke(Color.DODGERBLUE);
        g.setLineWidth(1.5);
        g.strokeRect(x, y, width, height);
        g.setLineDashes(null);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;
        dragging = false;

        Point2D p = s.getBase().sceneToLocal(e.getSceneX(), e.getSceneY());
        double x = Math.min(sx, p.getX());
        double y = Math.min(sy, p.getY());
        double width  = Math.abs(p.getX() - sx);
        double height = Math.abs(p.getY() - sy);

        if (width < 1 || height < 1) {
            s.clearOverlay();
            return;
        }

        var full = s.getBase().snapshot(null, null);
        var pr = full.getPixelReader();
        int ix = (int) Math.max(0, Math.min(x, full.getWidth()  - 1));
        int iy = (int) Math.max(0, Math.min(y, full.getHeight() - 1));
        int iw = (int) Math.min(width,  full.getWidth()  - ix);
        int ih = (int) Math.min(height, full.getHeight() - iy);

        WritableImage selection = new WritableImage(pr, ix, iy, iw, ih);

        s.getBase().getGraphicsContext2D().clearRect(ix, iy, iw, ih);
        s.setSelection(selection);
        s.setSelPos(ix, iy);
        s.clearOverlay();
        s.drawOverlayImage(selection, ix, iy);

        if (moveTool != null) moveTool.takeSelectionMode();
    }
}
