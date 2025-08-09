package com.example.paint;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public final class FloatingTooltip {
    private FloatingTooltip() {}

    public static void attach(Node node, String text) {
        Tooltip tip = new Tooltip(text);
        tip.setAutoHide(true);

        final Duration delay = Duration.millis(300);
        final double offsetX = 12, offsetY = 12;

        PauseTransition showDelay = new PauseTransition(delay);
        final double[] lastScreenXY = new double[2];
        final boolean[] showing = { false };

        node.setOnMouseEntered(e -> {
            lastScreenXY[0] = e.getScreenX();
            lastScreenXY[1] = e.getScreenY();
            showDelay.setOnFinished(ev -> {
                if (node.getScene() == null || node.getScene().getWindow() == null) return;
                tip.show(node.getScene().getWindow(), lastScreenXY[0] + offsetX, lastScreenXY[1] + offsetY);
                showing[0] = true;
            });
            showDelay.playFromStart();
        });

        node.setOnMouseMoved(e -> {
            lastScreenXY[0] = e.getScreenX();
            lastScreenXY[1] = e.getScreenY();
            if (showing[0] && tip.isShowing()) {
                tip.setX(lastScreenXY[0] + offsetX);
                tip.setY(lastScreenXY[1] + offsetY);
            }
        });

        node.setOnMouseExited(e -> {
            showDelay.stop();
            tip.hide();
            showing[0] = false;
        });

        node.setOnMousePressed(e -> {
            showDelay.stop();
            tip.hide();
            showing[0] = false;
        });
    }
}
