package com.example.paint;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;

public class ColorPalette extends VBox {

    private final ColorPicker target;

    public ColorPalette(String title, ColorPicker target, Color[] colors, int columns, int swatchSize) {
        this.target = target;

        Label header = new Label(title);
        header.getStyleClass().add("subtle-header");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        for (int i = 0; i < colors.length; i++) {
            int col = i % columns;
            int row = i / columns;
            Button swatch = makeSwatch(colors[i], swatchSize);
            grid.add(swatch, col, row);
        }

        setSpacing(8);
        setPadding(new Insets(4, 0, 0, 0));
        getChildren().addAll(header, grid);
    }

    private Button makeSwatch(Color c, int size) {
        Button b = new Button();
        b.setMinSize(size, size);
        b.setPrefSize(size, size);
        b.setMaxSize(size, size);
        b.setCursor(Cursor.HAND);
        b.getStyleClass().add("swatch");

        // Use inline style so it’s not overridden by theme css
        String bg = toRgba(c);
        b.setStyle("-fx-background-color: " + bg + ";" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: rgba(0,0,0,0.25);" +
                "-fx-border-radius: 6;" +
                "-fx-padding: 0;");

        String hex = toHex(c);
        Tooltip.install(b, new Tooltip(hex));

        b.setOnAction(e -> {
            // 1) Update the picker UI
            target.setValue(c);
            // 2) Fire the picker’s value property so CanvasState listeners update tools immediately
            // (ColorPicker already fires valueProperty; nothing else needed)
            // Optional: return focus to canvas so keyboard shortcuts continue to work
            b.getScene().getRoot().requestFocus();
        });

        return b;
    }

    private static String toRgba(Color c) {
        int r = (int)Math.round(c.getRed()*255);
        int g = (int)Math.round(c.getGreen()*255);
        int b = (int)Math.round(c.getBlue()*255);
        String a = String.format("%.3f", c.getOpacity());
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }

    private static String toHex(Color c) {
        int r = (int)Math.round(c.getRed()*255);
        int g = (int)Math.round(c.getGreen()*255);
        int b = (int)Math.round(c.getBlue()*255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    // Convenience palettes
    public static Color[] vibrant24() {
        return new Color[]{
                Color.web("#000000"), Color.web("#1E293B"), Color.web("#475569"),
                Color.web("#EF4444"), Color.web("#F97316"), Color.web("#F59E0B"),
                Color.web("#22C55E"), Color.web("#10B981"), Color.web("#06B6D4"),
                Color.web("#3B82F6"), Color.web("#6366F1"), Color.web("#8B5CF6"),
                Color.web("#EC4899"), Color.web("#F43F5E"), Color.web("#A3E635"),
                Color.web("#84CC16"), Color.web("#14B8A6"), Color.web("#0EA5E9"),
                Color.web("#60A5FA"), Color.web("#A78BFA"), Color.web("#F472B6"),
                Color.web("#FB7185"), Color.web("#EAB308"), Color.web("#FACC15")
        };
    }

    public static Color[] neutrals10() {
        return new Color[]{
                Color.web("#FFFFFF"), Color.web("#F8FAFC"), Color.web("#F1F5F9"),
                Color.web("#E2E8F0"), Color.web("#CBD5E1"), Color.web("#94A3B8"),
                Color.web("#64748B"), Color.web("#475569"), Color.web("#334155"),
                Color.web("#0F172A")
        };
    }
}
