package com.example.paint;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo/Redo where the UNDO stack's last element is always the current state.
 * - Call push() once after finishing a stroke/shape (mouse released).
 * - New edits clear the redo stack.
 * - Supports transparent canvases.
 */
public class HistoryManager {
    private final CanvasState state;
    private final Deque<WritableImage> undo = new ArrayDeque<>(); // top = last
    private final Deque<WritableImage> redo = new ArrayDeque<>(); // top = last
    private final int maxDepth;

    public HistoryManager(CanvasState state) { this(state, 200); }

    public HistoryManager(CanvasState state, int maxDepth) {
        this.state = state;
        this.maxDepth = Math.max(1, maxDepth);
        // initial snapshot as current
        undo.addLast(snapshotTransparent());
    }

    /** Capture the current canvas as a new state (call AFTER finishing the stroke). */
    public void push() {
        WritableImage snap = snapshotTransparent();
        if (undo.size() >= maxDepth) undo.removeFirst(); // drop oldest
        undo.addLast(snap);      // new current
        redo.clear();            // new branch: redo invalid
    }

    public boolean canUndo() { return undo.size() > 1; } // keep at least one current
    public boolean canRedo() { return !redo.isEmpty(); }

    /** Step back to previous state. */
    public void undo() {
        if (!canUndo()) return;
        // move current -> redo
        WritableImage current = undo.removeLast();
        redo.addLast(current);
        // draw new current (previous)
        WritableImage previous = undo.peekLast();
        if (previous != null) draw(previous);
    }

    /** Step forward to next state, if any. */
    public void redo() {
        if (!canRedo()) return;
        WritableImage next = redo.removeLast();
        draw(next);
        // make it the new current
        if (undo.size() >= maxDepth) undo.removeFirst();
        undo.addLast(next);
    }

    /** Clear all history and start fresh from current canvas. */
    public void clear() {
        undo.clear();
        redo.clear();
        undo.addLast(snapshotTransparent());
    }

    // --- internals ----
    private WritableImage snapshotTransparent() {
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return state.getBase().snapshot(sp, null);
    }

    private void draw(WritableImage img) {
        GraphicsContext g = state.getBase().getGraphicsContext2D();
        g.clearRect(0, 0, state.getBase().getWidth(), state.getBase().getHeight()); // true transparent clear
        g.drawImage(img, 0, 0);
    }
}
