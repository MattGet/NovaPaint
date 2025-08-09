package com.example.paint;

import javafx.scene.Node;
import javafx.scene.control.ToggleButton;

public class IconToggleButton extends ToggleButton {
    public IconToggleButton(Node graphic) {
        setGraphic(graphic);
        getStyleClass().add("icon-toggle");
        setPrefSize(36, 36);
        setMinSize(36, 36);
        setMaxSize(36, 36);
        setFocusTraversable(false);
    }
}
