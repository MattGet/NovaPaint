package com.example.paint;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/** Minimal, crisp SVG icons that work offline (no external fonts). */
public final class IconFactory {
    private IconFactory(){}

    private static Node path(String d) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.getStyleClass().add("icon");
        return p;
    }

    public static Node pencil() {
        return path("M3 14l9-9 3 3-9 9H3v-3zm12.5-7.5l-3-3 1.5-1.5a2 2 0 0 1 2.8 0l0.2 0.2a2 2 0 0 1 0 2.8L15.5 6.5z");
    }

    public static Node eraser() {
        return path("M3 12l6-6a2 2 0 0 1 3 0l5 5a2 2 0 0 1 0 3l-4 4H7l-4-4 0-2z");
    }

    public static Node line() {
        return path("M3 17L17 3");
    }

    public static Node rect() {
        return path("M4 4h12v12H4z");
    }

    public static Node ellipse() {
        return path("M10 4a6 6 0 1 1 0 12a6 6 0 1 1 0-12z");
    }

    public static Node polygon() {
        return path("M10 3l6 4-2 7H6L4 7z");
    }

    public static Node spray() {
        return path("M5 5h2v2H5zM9 5h2v2H9zM13 5h2v2h-2zM7 9h2v2H7zM11 9h2v2h-2zM9 13h2v2H9z");
    }

    public static Node bucket() {
        return path("M4 8l6-6 6 6v1H4V8zm2 2h8l-1 6a3 3 0 0 1-6 0L6 10z");
    }

    public static Node select() {
        return path("M4 4h4v2H6v2H4V4zm8 0h4v4h-2V6h-2V4zM4 12h2v2h2v2H4v-4zm8 4v-2h2v-2h2v4h-4z");
    }

    public static Node move() {
        return path("M10 2l3 3h-2v4h4V7l3 3-3 3v-2h-4v4h2l-3 3-3-3h2v-4H6v2L3 10l3-3v2h4V5H8l2-3z");
    }

    public static Node dropper() {
        return path("M14 3l2 2-8 8-2 1 1-2 8-8zM5 14l2 2-3 1 1-3z");
    }

    public static Node text() {
        return path("M4 5h12v2h-5v8H9V7H4z");
    }

    public static Node hand() {
        return path("M6 14V7a2 2 0 1 1 4 0v3h1V6a2 2 0 1 1 4 0v5h1V9a2 2 0 1 1 4 0v6a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z");
    }

    public static Node open() {
        return path("M4 4h8v2H6v8H4V4zm5 4h7l-3 3 3 3H9V8z");
    }

    public static Node save() {
        return path("M4 4h10l2 2v10H4V4zm2 2v2h6V6H6zm0 4h8v4H6v-4z");
    }

    public static Node undo() {
        return path("M6 7V4L2 8l4 4V9h5a5 5 0 1 1 0 10h-1v-2h1a3 3 0 1 0 0-6H6z");
    }

    public static Node redo() {
        return path("M14 7V4l4 4-4 4V9H9a5 5 0 1 0 0 10h1v-2H9a3 3 0 1 1 0-6h5z");
    }

    public static Node clear() {
        return path("M4 6h12v2H4V6zm2 4h8l-1 6H7l-1-6z");
    }

    public static Node sunMoon() {
        return path("M6 10a4 4 0 1 0 8 0 4 4 0 1 0-8 0z M2 10h2M16 10h2M10 2v2M10 16v2M4.5 4.5l1.4 1.4M14.1 14.1l1.4 1.4M14.1 5.9l1.4-1.4M4.5 15.5l1.4-1.4");
    }
}
