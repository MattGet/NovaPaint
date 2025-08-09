package com.example.paint;

import javafx.scene.image.WritableImage;

import java.util.ArrayDeque;
import java.util.Deque;

public class HistoryManager {
    private final CanvasState state;
    private final Deque<WritableImage> undo = new ArrayDeque<>();
    private final Deque<WritableImage> redo = new ArrayDeque<>();

    public HistoryManager(CanvasState state) {
        this.state = state;
        push(); // initial snapshot
    }

    public void push() {
        undo.push(state.getBase().snapshot(null, null));
        redo.clear();
    }

    public void undo() {
        if (undo.isEmpty()) return;
        var current = state.getBase().snapshot(null, null);
        redo.push(current);
        var img = undo.removeLast();
        state.getBase().getGraphicsContext2D().drawImage(img, 0, 0);
    }

    public void redo() {
        if (redo.isEmpty()) return;
        var img = redo.removeLast();
        undo.push(state.getBase().snapshot(null, null));
        state.getBase().getGraphicsContext2D().drawImage(img, 0, 0);
    }

    public void clear() {
        undo.clear();
        redo.clear();
        push(); // snapshot fresh state so undo starts from here
    }

}
