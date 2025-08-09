package com.example.paint;

import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class TextTool implements Tool {
    @Override public String getName(){ return "Text"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        h.push();
        var dlg = new TextInputDialog("");
        dlg.setTitle("Text"); dlg.setHeaderText("Enter text");
        dlg.showAndWait().ifPresent(txt -> {
            var g = s.getBase().getGraphicsContext2D();
            FontWeight fw = s.isBold()? FontWeight.BOLD : FontWeight.NORMAL;
            FontPosture fp = s.isItalic()? FontPosture.ITALIC : FontPosture.REGULAR;
            g.setFill(s.getStroke());
            g.setFont(Font.font(s.getFontFamily(), fw, fp, s.getFontSize()));
            g.fillText(txt, e.getX(), e.getY());
        });
    }
}
