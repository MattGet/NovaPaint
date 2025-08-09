package com.example.paint;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class SelectTool implements Tool {
    private double sx, sy, ex, ey;
    private boolean dragging;

    // persistent selection rect for cut (int bounds)
    private int selX, selY, selW, selH;

    @Override public String getName(){ return "Select"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        // If anything was left on the overlay (ghost/marquee), just wipe it.
        // We DO NOT commit overlay here to avoid baking UI or stale ghosts.
        s.clearOverlay();
        s.setSelection(null);

        dragging = true;
        sx = ex = e.getX();
        sy = ey = e.getY();
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;
        ex = e.getX();
        ey = e.getY();

        double x = Math.min(sx, ex), y = Math.min(sy, ey);
        double w = Math.abs(ex - sx), hgt = Math.abs(ey - sy);

        GraphicsContext g = s.getOverlay().getGraphicsContext2D();
        s.clearOverlay();

        // translucent highlight
        g.setFill(Color.color(0.20, 0.60, 1.0, 0.20));
        g.fillRect(x, y, w, hgt);
        drawMarquee(g, x, y, w, hgt);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;
        dragging = false;

        double rx = Math.min(sx, ex), ry = Math.min(sy, ey);
        double rw = Math.abs(ex - sx), rh = Math.abs(ey - sy);
        if (rw < 1 || rh < 1) { s.clearOverlay(); s.setSelection(null); return; }

        // clamp to canvas
        int cw = (int) s.getBase().getWidth();
        int ch = (int) s.getBase().getHeight();
        selX = (int) Math.max(0, Math.min(rx, cw - 1));
        selY = (int) Math.max(0, Math.min(ry, ch - 1));
        selW = (int) Math.max(1, Math.min(rw, cw - selX));
        selH = (int) Math.max(1, Math.min(rh, ch - selY));

        // snapshot full canvas (transparent-aware) and crop → ghost
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage full = s.getBase().snapshot(sp, null);
        WritableImage snap = new WritableImage(full.getPixelReader(), selX, selY, selW, selH);

        // show ghost + marquee (user can copy/cut; we’ll hide it after those actions)
        s.clearOverlay();
        s.drawOverlayImage(snap, selX, selY);
        drawMarquee(s.getOverlay().getGraphicsContext2D(), selX, selY, selW, selH);

        s.setSelection(snap);
        s.setSelPos(selX, selY);
        s.setStatus("Selection ready (Ctrl+C / Ctrl+X / Ctrl+V)");
    }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        s.clearOverlay();
        s.setSelection(null);
    }

    /* ===== Clipboard actions ===== */

    /** Copy current selection to system clipboard; hide marquee/ghost afterward. */
    public void copySelection(CanvasState s) {
        if (s.getSelection() == null) return;
        ClipboardContent cc = new ClipboardContent();
        cc.putImage(s.getSelection()); // preserves alpha
        Clipboard.getSystemClipboard().setContent(cc);

        // Hide marquee/ghost; nothing is committed to base here.
        s.clearOverlay();
        s.setSelection(null);
        s.setStatus("Copied (you can paste multiple times)");
    }

    /** Cut selection: copy to clipboard, clear that rect on base (transparent), push once, hide marquee. */
    public void cutSelection(CanvasState s, HistoryManager h) {
        if (s.getSelection() == null) return;

        ClipboardContent cc = new ClipboardContent();
        cc.putImage(s.getSelection());
        Clipboard.getSystemClipboard().setContent(cc);

        s.getBase().getGraphicsContext2D().clearRect(selX, selY, selW, selH);
        h.push(); // one history entry

        s.clearOverlay();
        s.setSelection(null);
        s.setStatus("Cut (you can paste multiple times)");
    }

    /** Paste under mouse (centered) -> draw DIRECTLY to BASE, push once; no overlay ghost kept. */
    public void pasteFromClipboard(CanvasState s, HistoryManager h) {
        var cb = Clipboard.getSystemClipboard();
        var src = cb.getImage();
        if (src == null) { s.setStatus("Clipboard has no image"); return; }

        int w = (int) Math.ceil(src.getWidth());
        int hImg = (int) Math.ceil(src.getHeight());
        var pr = src.getPixelReader();
        if (pr == null || w <= 0 || hImg <= 0) { s.setStatus("Clipboard image unreadable"); return; }
        WritableImage img = new WritableImage(pr, 0, 0, w, hImg);

        // Center under last mouse (clamped)
        double cx = s.getLastMouseX();
        double cy = s.getLastMouseY();
        var topLeft = s.clampPasteTopLeft(w, hImg, cx - w/2.0, cy - hImg/2.0);
        double px = topLeft.getX(), py = topLeft.getY();

        // Draw to BASE immediately (no overlay), then push history
        s.getBase().getGraphicsContext2D().drawImage(img, px, py);
        h.push();

        // Clear any overlay/marquee and selection state (paste is a finished action)
        s.clearOverlay();
        s.setSelection(null);

        s.setStatus("Pasted (committed). You can paste again or move it with a new selection.");
    }

    /* ===== Marquee helper ===== */
    public static void drawMarquee(GraphicsContext g, double x, double y, double w, double h) {
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        g.setLineDashes(6);
        g.setLineDashOffset(0);
        g.strokeRect(x, y, w, h);

        g.setStroke(Color.BLACK);
        g.setLineWidth(1.5);
        g.setLineDashes(6);
        g.setLineDashOffset(3);
        g.strokeRect(x, y, w, h);

        g.setLineDashes(0);
        g.setLineDashOffset(0);
    }

    // Optional compatibility shim:
    public void setMoveTool(MoveTool moveTool) { }
}
