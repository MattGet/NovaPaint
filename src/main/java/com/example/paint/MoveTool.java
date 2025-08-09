package com.example.paint;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class MoveTool implements Tool {
    private double mx, my;          // mouse anchor (set on press)
    private boolean dragging;
    private boolean moved;

    // Selection snapshot & geometry for the CURRENT drag only
    private WritableImage selImg;   // fresh snapshot from BASE at press time
    private double origX, origY;    // selection top-left at press
    private double currX, currY;    // ghost top-left while dragging
    private double selW, selH;

    @Override public String getName(){ return "Move"; }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        if (s.getSelection() == null) {
            s.setStatus("Move: no active selection. Use Select first.");
        } else {
            s.setStatus("Move: click-drag to reposition, release to commit, Esc to cancel.");
            // show marquee for current selection; do NOT touch the base
            s.clearOverlay();
            SelectTool.drawMarquee(
                    s.getOverlay().getGraphicsContext2D(),
                    s.getSelX(), s.getSelY(),
                    s.getSelection().getWidth(), s.getSelection().getHeight()
            );
        }
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        if (s.getSelection() == null) return;

        mx = e.getX();
        my = e.getY();
        dragging = true;
        moved = false;

        origX = s.getSelX();
        origY = s.getSelY();
        selW  = s.getSelection().getWidth();
        selH  = s.getSelection().getHeight();

        // fresh snapshot from base to avoid duplication on repeated moves
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage full = s.getBase().snapshot(sp, null);
        selImg = new WritableImage(full.getPixelReader(), (int)origX, (int)origY, (int)selW, (int)selH);

        currX = origX;
        currY = origY;

        // cut original pixels from base
        s.getBase().getGraphicsContext2D().clearRect(origX, origY, selW, selH);

        // draw ghost + marquee on overlay
        s.clearOverlay();
        s.drawOverlayImage(selImg, currX, currY);
        SelectTool.drawMarquee(s.getOverlay().getGraphicsContext2D(), currX, currY, selW, selH);

        s.setStatus("Moving selection…");
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging || selImg == null) return;

        double dx = e.getX() - mx;
        double dy = e.getY() - my;
        currX = origX + dx;
        currY = origY + dy;
        if (!moved && (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5)) moved = true;

        s.clearOverlay();
        s.drawOverlayImage(selImg, currX, currY);
        SelectTool.drawMarquee(s.getOverlay().getGraphicsContext2D(), currX, currY, selW, selH);
        s.setSelPos(currX, currY);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging || selImg == null) return;
        dragging = false;

        if (moved) {
            // commit only the ghost (no marquee)
            s.clearOverlay();
            s.drawOverlayImage(selImg, currX, currY);
            s.commitOverlay();
            h.push();

            // keep selection active at new spot; redraw marquee
            s.setSelection(selImg);
            s.setSelPos(currX, currY);
            SelectTool.drawMarquee(s.getOverlay().getGraphicsContext2D(), currX, currY, selW, selH);

            s.setStatus("Selection moved (still active)");
            selImg = null; // next drag will resnapshot
        } else {
            // restore original pixels (no history)
            s.clearOverlay();
            s.getBase().getGraphicsContext2D().drawImage(selImg, origX, origY);

            s.setSelection(selImg);
            s.setSelPos(origX, origY);
            SelectTool.drawMarquee(s.getOverlay().getGraphicsContext2D(), origX, origY, selW, selH);

            s.setStatus("Move canceled");
            selImg = null;
        }
    }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        if (!dragging || selImg == null) { dragging = false; return; }
        s.clearOverlay();
        s.getBase().getGraphicsContext2D().drawImage(selImg, origX, origY);
        // keep selection at original spot (with marquee while still in Move)
        s.setSelection(selImg);
        s.setSelPos(origX, origY);
        SelectTool.drawMarquee(s.getOverlay().getGraphicsContext2D(), origX, origY, selW, selH);
        selImg = null;
        dragging = false;
        s.setStatus("Move canceled");
    }

    @Override
    public void onDeselect(CanvasState s, HistoryManager h) {
        // If a drag was mid-flight, restore pixels first
        if (dragging && selImg != null) {
            s.clearOverlay();
            s.getBase().getGraphicsContext2D().drawImage(selImg, origX, origY);
        }
        // ALWAYS remove marquee and clear active selection when leaving Move
        s.clearOverlay();
        s.setSelection(null);
        dragging = false;
        moved = false;
        selImg = null;
        s.setStatus("Move: deselected");
    }
}
