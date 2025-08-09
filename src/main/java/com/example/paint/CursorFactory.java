package com.example.paint;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

public class CursorFactory {

    /**
     * Generates a circular brush ring cursor with a transparent background.
     *
     * @param ringColor  Color of the ring outline
     * @param diameter   Outer diameter of the ring
     * @param strokeWidth Width of the ring stroke
     * @return ImageCursor with transparent background
     */
    public static Cursor brushRing(Color ringColor, double diameter, double strokeWidth) {
        double size = Math.ceil(diameter + strokeWidth * 2);
        Canvas cursorCanvas = new Canvas(size, size);
        GraphicsContext gc = cursorCanvas.getGraphicsContext2D();

        gc.setStroke(ringColor);
        gc.setLineWidth(strokeWidth);
        gc.setLineCap(StrokeLineCap.ROUND);

        // draw centered circle
        double inset = strokeWidth / 2.0;
        gc.strokeOval(inset, inset, size - strokeWidth, size - strokeWidth);

        // take transparent snapshot
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT); // <-- transparent background
        WritableImage img = new WritableImage((int) size, (int) size);
        cursorCanvas.snapshot(params, img);

        // hotspot in center of the circle
        return new ImageCursor(img, size / 2, size / 2);
    }
}
