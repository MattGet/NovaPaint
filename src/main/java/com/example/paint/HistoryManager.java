package com.example.paint;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;

/** Transparent-safe undo/redo using canvas snapshots. */
public class HistoryManager {
    private final CanvasState state;
    private final Deque<WritableImage> undo = new ArrayDeque<>();
    private final Deque<WritableImage> redo = new ArrayDeque<>();
    private final int maxDepth;

    public HistoryManager(CanvasState state) { this(state, 80); }

    public HistoryManager(CanvasState state, int maxDepth) {
        this.state = state;
        this.maxDepth = Math.max(1, maxDepth);
        push(); // initial snapshot
    }

    /** Capture current canvas into undo stack. Call BEFORE modifying pixels. */
    public void push() {
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);                  // preserve alpha
        WritableImage snap = state.getBase().snapshot(sp, null);
        if (undo.size() >= maxDepth) undo.removeLast();
        undo.push(snap);
        redo.clear();
    }

    public boolean canUndo() { return undo.size() > 1; }
    public boolean canRedo() { return !redo.isEmpty(); }

    public void undo() {
        if (!canUndo()) return;
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage current = state.getBase().snapshot(sp, null);
        redo.push(current);

        undo.pop(); // drop current
        WritableImage prev = undo.peek();
        if (prev != null) draw(prev);
    }

    public void redo() {
        if (!canRedo()) return;
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage current = state.getBase().snapshot(sp, null);
        undo.push(current);

        WritableImage img = redo.pop();
        draw(img);
    }

    public void clear() {
        undo.clear();
        redo.clear();
        push();
    }

    private void draw(WritableImage img) {
        GraphicsContext g = state.getBase().getGraphicsContext2D();
        // clear to transparent, then draw snapshot (which may itself contain transparency)
        g.clearRect(0,0,state.getBase().getWidth(), state.getBase().getHeight());
        g.drawImage(img, 0, 0);
    }
}
