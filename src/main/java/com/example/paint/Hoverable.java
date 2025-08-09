package com.example.paint;

public interface Hoverable {
    /** Mouse moved (no buttons pressed), in SCENE coordinates. */
    void onMove(CanvasState s, double sceneX, double sceneY);
}
