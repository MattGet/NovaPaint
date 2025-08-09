package com.example.paint;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

/**
 * Sharp 24x24 icons, consistent stroke/fill.
 * - Filled icons use style class "icon"
 * - Stroked icons (line-only) also add "icon-stroke"
 *
 * Pair with CSS:
 *   .icon { -fx-fill: -np-ink; -fx-stroke: -np-ink; -fx-stroke-width: 0; }
 *   .icon-stroke { -fx-fill: transparent; -fx-stroke: -np-ink; -fx-stroke-width: 2; -fx-stroke-line-cap: round; }
 */
public final class IconFactory {
    private IconFactory() {}

    private static Node filled(String d) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.getStyleClass().add("icon");
        return p;
    }

    private static Node stroked(String d) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.getStyleClass().addAll("icon", "icon-stroke");
        return p;
    }

    // ---- Tools ----

    /** Pencil (tilted nib) */
    public static Node pencil() {
        // body + nib cut; simple but readable
        return filled("M3 15.75L14.75 4a2.5 2.5 0 0 1 3.54 0l1.71 1.71a2.5 2.5 0 0 1 0 3.54L8.29 21H3v-5.25zM6.5 18.5l9.9-9.9 1.5 1.5-9.9 9.9H6.5z");
    }

    /** Eraser (two-part block) */
    public static Node eraser() {
        return filled("M4.3 14.7L12.0 7.0a3 3 0 0 1 4.24 0l2.76 2.76a3 3 0 0 1 0 4.24L11.3 22H6.5L4.3 19.8a3 3 0 0 1 0-4.24zM6.7 18.4l2.2 2.2h2.4l6.2-6.2a1.5 1.5 0 0 0 0-2.12L15 9.5a1.5 1.5 0 0 0-2.12 0l-6.2 6.2z");
    }

    /** Line (uses stroke) */
    public static Node line() { return stroked("M4 19L20 5"); }

    /** Rectangle */
    public static Node rect() { return filled("M5 6h14a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z"); }

    /** Ellipse / circle */
    public static Node ellipse() { return filled("M12 5a7 7 0 1 1 0 14a7 7 0 1 1 0-14z"); }

    /** Polygon (pentagon) */
    public static Node polygon() { return filled("M12 3l7.2 4.6-2.7 8.5H7.5L4.8 7.6z"); }

    /** Spray (nozzle + dots) */
    public static Node spray() {
        // Stylized spray can (body + nozzle) with a simple mist cluster
        return filled(
                // can body
                "M7 9h8a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z" +
                        // cap/nozzle block
                        "M9 6h6a1 1 0 0 1 1 1v2H8V7a1 1 0 0 1 1-1z" +
                        // nozzle dot
                        "M13.5 7.75a.75.75 0 1 1-1.5 0a.75.75 0 0 1 1.5 0z" +
                        // mist dots
                        "M19 9.5a1 1 0 1 1-2 0a1 1 0 0 1 2 0z" +
                        "M20.5 12a.9.9 0 1 1-1.8 0a.9.9 0 0 1 1.8 0z" +
                        "M19 14.5a.8.8 0 1 1-1.6 0a.8.8 0 0 1 1.6 0z" +
                        "M21 16a.7.7 0 1 1-1.4 0a.7.7 0 0 1 1.4 0z"
        );
    }

    /** Paint bucket (tilt + drip) */
    public static Node bucket() {
        return filled("M7 9l5-5 6 6-5 5H7V9zm11.5 1.5l1.9 1.9c.9.9.9 2.4 0 3.3-.9.9-2.4.9-3.3 0-.9-.9-.9-2.4 0-3.3l1.4-1.4z");
    }

    /** Select (dotted marquee + arrows) */
    public static Node select() {
        return filled("M4 6h2v2H4V6zm4 0h4v2H8V6zm6 0h2v2h-2V6zM4 10h2v4H4v-4zm12 0h2v4h-2v-4zM4 16h2v2H4v-2zm4 0h4v2H8v-2zm6 0h2v2h-2v-2zM11 3h2v2h-2V3zm0 16h2v2h-2v-2z");
    }

    /** Move (4-way arrows) */
    public static Node move() { return filled("M11 2h2l3 3h-2v4h4V7l3 3-3 3v-2h-4v4h2l-3 3h-2l-3-3h2v-4H7v2l-3-3 3-3v2h4V5H8l3-3z"); }

    /** Eyedropper */
    public static Node dropper() { return filled("M15.5 3a2.5 2.5 0 0 1 1.77.73l1.5 1.5a2.5 2.5 0 0 1 0 3.54l-1.5 1.5-2-2-7.29 7.3-3.48.87.87-3.48 7.3-7.29-2-2 1.5-1.5A2.5 2.5 0 0 1 15.5 3z"); }

    /** Text (T glyph) */
    public static Node text() { return filled("M5 6h14v2h-6v10h-2V8H5V6z"); }

    /** Hand / Pan */
    public static Node hand() { return filled("M6 13V8a2 2 0 1 1 4 0v2h1V7a2 2 0 1 1 4 0v3h1V9a2 2 0 1 1 4 0v6a6 6 0 0 1-6 6H10a6 6 0 0 1-6-6z"); }

    // ---- App / File ----

    public static Node open() { return filled("M4 5h10l3 3v11H4V5zm9 7h6l-3 3 3 3h-6v-6z"); }

    public static Node save() { return filled("M5 5h12l2 2v12H5V5zm2 2v3h8V7H7zm0 5h10v5H7v-5z"); }

    public static Node undo() { return filled("M7 8V5L2 10l5 5v-3h6a5 5 0 1 1 0 10h-1v-2h1a3 3 0 1 0 0-6H7z"); }

    public static Node redo() { return filled("M17 8V5l5 5-5 5v-3h-6a5 5 0 1 0 0 10h1v-2h-1a3 3 0 1 1 0-6h6z"); }

    public static Node clear() { return filled("M6 7h12v2H6V7zm2 4h8l-1.2 7H9.2L8 11z"); }

    public static Node sunMoon() {
        // Crescent moon (Feather-style). Reads clearly at 16–20px, tinted via .icon CSS.
        // Path: m21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z
        return filled("M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z");
    }
}
