package com.example.paint;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class TextTool implements Tool {
    @Override public String getName(){ return "Text"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        var g = s.getBase().getGraphicsContext2D();
        String family = s.getFontFamily();
        int size = s.getFontSize();
        FontWeight fw = s.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture fp = s.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        g.setFont(Font.font(family, fw, fp, size));
        g.setFill(s.getStroke()); // use stroke color for text fill
        g.fillText("Text", e.getX(), e.getY()); // replace with your text input
        h.push(); // single history entry
    }

    @Override public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) { }
    @Override public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) { }
}
