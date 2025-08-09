package com.example.paint;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * Text tool with popup editor:
 *  - Click to show a small TextField + OK/Cancel near the cursor.
 *  - Enter/OK commits to the base canvas (using current font/fill/stroke) and pushes history.
 *  - Esc/Cancel closes without committing.
 */
public class TextTool implements Tool {

    private ContextMenu popup;
    private TextField input;
    private double clickX, clickY;   // canvas-space coordinates where text should be drawn
    private String lastText = "";    // prefill convenience

    @Override public String getName() { return "Text"; }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        s.setStatus("Text: click to place. Type, then Enter or OK to commit. Esc/Cancel to abort.");
        s.getViewport().setCursor(Cursor.TEXT);
    }

    @Override
    public void onDeselect(CanvasState s, HistoryManager h) {
        hidePopup();
        s.getViewport().setCursor(Cursor.DEFAULT);
    }

    @Override
    public void onCancel(CanvasState s, HistoryManager h) {
        hidePopup();
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        // Record where to draw (canvas coordinates from overlay)
        clickX = e.getX();
        clickY = e.getY();

        showPopupAtMouse(s, h, e);
    }

    @Override public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) { }
    @Override public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) { }

    /* ---------- Popup UI ---------- */

    private void showPopupAtMouse(CanvasState s, HistoryManager h, MouseEvent e) {
        hidePopup(); // ensure only one

        input = new TextField(lastText);
        input.setPromptText("Enter text…");
        input.setPrefColumnCount(20);

        Button ok = new Button("OK");
        Button cancel = new Button("Cancel");

        ok.setDefaultButton(true);     // Enter triggers OK
        cancel.setCancelButton(true);  // Esc triggers Cancel

        ok.setOnAction(ae -> commitIfAny(s, h));
        cancel.setOnAction(ae -> hidePopup());

        // Also handle keys while focused in the TextField
        input.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                ke.consume();
                commitIfAny(s, h);
            } else if (ke.getCode() == KeyCode.ESCAPE) {
                ke.consume();
                hidePopup();
            }
        });

        HBox content = new HBox(8, input, ok, cancel);
        content.getStyleClass().add("text-popup");
        content.setStyle("""
                -fx-padding: 6;
                -fx-background-color: rgba(255,255,255,0.98);
                -fx-background-radius: 6;
                -fx-border-color: rgba(0,0,0,0.25);
                -fx-border-radius: 6;
                """);

        CustomMenuItem item = new CustomMenuItem(content, false); // don't auto-hide on click
        popup = new ContextMenu(item);

        // Position near the cursor (convert overlay local → screen)
        Point2D screen = s.getOverlay().localToScreen(e.getX(), e.getY());
        if (screen != null) {
            popup.show(s.getViewport(), screen.getX(), screen.getY());
        } else {
            // Fallback: show relative to viewport if screen coords unavailable
            popup.show(s.getViewport(), e.getScreenX(), e.getScreenY());
        }

        // Focus the text field ready to type
        input.requestFocus();
        input.selectAll();
    }

    private void hidePopup() {
        if (popup != null) {
            popup.hide();
            popup = null;
            input = null;
        }
    }

    private void commitIfAny(CanvasState s, HistoryManager h) {
        if (input == null) return;
        String text = input.getText();
        if (text == null || text.isBlank()) {
            hidePopup();
            s.setStatus("Text canceled");
            return;
        }
        commitText(s, h, text, clickX, clickY);
        lastText = text;
        hidePopup();
        s.setStatus("Text committed");
    }

    /* ---------- Drawing ---------- */

    private void commitText(CanvasState s, HistoryManager h, String text, double x, double y) {
        var g = s.getBase().getGraphicsContext2D();

        // Use the dedicated text color; ignore stroke entirely by default
        javafx.scene.paint.Color textColor =
                s.getTextColor() != null ? s.getTextColor() : javafx.scene.paint.Color.BLACK;

        javafx.scene.text.Font font = currentFont(s);
        g.setFont(font);

        double baselineY = y + font.getSize(); // draw from a top-left click
        g.setFill(textColor);
        g.fillText(text, x, baselineY);

        h.push(); // single history entry
    }



    private Font currentFont(CanvasState s) {
        String family = s.getFontFamilyBox().getValue();
        double size = s.getFontSizeSpinner().getValue();
        boolean bold = s.getBoldCheck().isSelected();
        boolean italic = s.getItalicCheck().isSelected();

        FontWeight w = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture p = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        return Font.font(family, w, p, size);
    }
}
