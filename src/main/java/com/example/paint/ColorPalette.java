package com.example.paint;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public final class ColorPalette extends VBox {
    /**
     * @param title      Section title text (e.g., "Quick Colors")
     * @param target     The ColorPicker to update when a swatch is clicked
     * @param colors     Colors to show (row-major fill)
     * @param columns    How many columns in the grid
     * @param swatchSize Diameter of each swatch (px)
     */
    public ColorPalette(String title, ColorPicker target, Color[] colors, int columns, double swatchSize) {
        getStyleClass().add("palette-section");
        setSpacing(8);

        var header = new javafx.scene.control.Label(title);
        header.getStyleClass().add("section");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("swatch-grid");
        grid.setHgap(8);
        grid.setVgap(8);

        for (int i = 0; i < colors.length; i++) {
            int r = i / columns;
            int c = i % columns;

            Color color = colors[i];
            StackPane swatch = buildSwatch(color, swatchSize);
            swatch.setOnMouseClicked(e -> target.setValue(color));
            Tooltip.install(swatch, new Tooltip(toHex(color)));

            grid.add(swatch, c, r);
        }

        getChildren().addAll(header, grid);
        setPadding(new Insets(6,0,0,0));
    }

    private static StackPane buildSwatch(Color c, double size) {
        Circle circle = new Circle(size / 2);
        circle.getStyleClass().add("swatch");
        circle.setFill(c);
        circle.setStrokeWidth(1.0);

        // auto-choose border contrast
        double luminance = 0.2126*c.getRed() + 0.7152*c.getGreen() + 0.0722*c.getBlue();
        circle.setStroke(luminance > 0.6 ? Color.gray(0,0.25) : Color.gray(1,0.35));

        StackPane sp = new StackPane(circle);
        sp.setPrefSize(size, size);
        sp.setMinSize(size, size);
        sp.setMaxSize(size, size);
        sp.getStyleClass().add("swatch-wrap");
        return sp;
    }

    private static String toHex(Color c) {
        int r = (int)Math.round(c.getRed()*255);
        int g = (int)Math.round(c.getGreen()*255);
        int b = (int)Math.round(c.getBlue()*255);
        return String.format("#%02X%02X%02X", r,g,b);
    }

    /** Handy presets */
    public static Color[] vibrant24() {
        return new Color[]{
                Color.web("#ef4444"), Color.web("#f97316"), Color.web("#f59e0b"), Color.web("#eab308"),
                Color.web("#84cc16"), Color.web("#22c55e"), Color.web("#10b981"), Color.web("#14b8a6"),
                Color.web("#06b6d4"), Color.web("#0ea5e9"), Color.web("#3b82f6"), Color.web("#6366f1"),
                Color.web("#8b5cf6"), Color.web("#a855f7"), Color.web("#d946ef"), Color.web("#ec4899"),
                Color.web("#f43f5e"), Color.web("#fb7185"), Color.web("#94a3b8"), Color.web("#64748b"),
                Color.web("#475569"), Color.web("#334155"), Color.web("#1f2937"), Color.web("#111827")
        };
    }

    public static Color[] neutrals10() {
        return new Color[]{
                Color.web("#ffffff"), Color.web("#f8fafc"), Color.web("#e2e8f0"), Color.web("#cbd5e1"), Color.web("#94a3b8"),
                Color.web("#64748b"), Color.web("#475569"), Color.web("#334155"), Color.web("#1f2937"), Color.web("#0f172a")
        };
    }
}
