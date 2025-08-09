package com.example.paint;

import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class ZoomTool implements Tool {
    // Tuning knobs
    private static final double MIN_FACTOR_PER_TICK = 0.5;
    private static final double MAX_FACTOR_PER_TICK = 2.0;
    private static final double BASE_SENSITIVITY = 0.0020;   // drag sensitivity (higher = faster)
    private static final double WHEEL_STEP = 1.10;           // wheel zoom step (10%)
    private static final double DBLCLICK_STEP = 1.6;         // double-click zoom step

    private double pivotSceneX;
    private double pivotSceneY;
    private double lastY;
    private boolean dragging;

    @Override public String getName() { return "Zoom"; }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        s.setStatus("Zoom: drag ↑/↓ to zoom at press point • Wheel zoom at cursor • Shift=fast, Ctrl/Cmd=fine • Double-click to zoom");
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        // Double-click behavior: quick in/out around cursor
        if (e.getClickCount() == 2) {
            double factor = (e.getButton() == MouseButton.SECONDARY) ? (1.0 / DBLCLICK_STEP) : DBLCLICK_STEP;
            s.zoomAtScene(e.getSceneX(), e.getSceneY(), factor);
            return;
        }

        // Start a drag-zoom gesture: fix pivot at press location (in scene coords)
        pivotSceneX = e.getSceneX();
        pivotSceneY = e.getSceneY();
        lastY = e.getY();
        dragging = true;
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!dragging) return;

        // Vertical delta controls zoom (incremental, to avoid jitter)
        double dy = e.getY() - lastY;
        lastY = e.getY();

        // Modifier-based sensitivity
        double scale = 1.0;
        if (e.isShiftDown()) scale *= 2.5;       // faster
        if (e.isControlDown() || e.isMetaDown()) // Ctrl (Win/Linux) or Cmd (macOS)
            scale *= 0.35;                       // finer

        // Exponential for smoothness; clamp per-tick factor
        double factor = Math.exp(BASE_SENSITIVITY * scale * dy * 100.0);
        factor = Math.max(MIN_FACTOR_PER_TICK, Math.min(MAX_FACTOR_PER_TICK, factor));

        s.zoomAtScene(pivotSceneX, pivotSceneY, factor);
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        dragging = false;
    }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        dragging = false;
    }

    // ---------------- Optional: attach smooth wheel zoom anywhere ----------------

    /**
     * Attach wheel-based zooming to a node (e.g., the overlay or viewport).
     * Zooms toward the cursor using CanvasState.zoomAtScene(...).
     */
    public static void installWheelZoom(Node node, CanvasState state) {
        node.addEventHandler(ScrollEvent.SCROLL, ev -> {
            // Only handle vertical scroll
            if (ev.getDeltaY() == 0) return;

            double factor = (ev.getDeltaY() > 0) ? WHEEL_STEP : (1.0 / WHEEL_STEP);

            // Modifiers: Shift = faster, Ctrl/Cmd = finer
            if (ev.isShiftDown())  factor = Math.pow(factor, 1.8);
            if (ev.isControlDown() || ev.isMetaDown()) factor = Math.pow(factor, 0.35);

            state.zoomAtScene(ev.getSceneX(), ev.getSceneY(), factor);
            ev.consume();
        });
    }
}

