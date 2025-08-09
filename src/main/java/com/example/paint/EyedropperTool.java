package com.example.paint;

import javafx.scene.input.MouseEvent;

public class EyedropperTool implements Tool {
    @Override public String getName(){ return "Eyedropper"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        var snap = s.getBase().snapshot(null, null);
        int ix = (int)e.getX(), iy = (int)e.getY();
        if (ix<0 || iy<0 || ix>=snap.getWidth() || iy>=snap.getHeight()) return;
        var c = snap.getPixelReader().getColor(ix, iy);
        s.getStrokePicker().setValue(c);
        s.setStatus("Picked " + c);
    }
}
