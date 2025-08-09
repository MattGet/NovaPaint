package com.example.paint;

import javafx.scene.input.MouseEvent;

public interface Tool {
    default String getName(){ return getClass().getSimpleName(); }

    default void onSelect(CanvasState s, HistoryManager h) {}
    default void onDeselect(CanvasState s, HistoryManager h) {}

    default void onPress(CanvasState s, HistoryManager h, MouseEvent e) {}
    default void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {}
    default void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {}
    default void onCancel(CanvasState s, HistoryManager h) {}
}
