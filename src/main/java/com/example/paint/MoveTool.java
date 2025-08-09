package com.example.paint;

import javafx.scene.input.MouseEvent;

public class MoveTool implements Tool {
    private double lastX, lastY;

    @Override public String getName(){ return "Move"; }

    public void takeSelectionMode(){ /* marker; nothing else required */ }

    @Override
    public void onSelect(CanvasState s, HistoryManager h) {
        s.clearOverlay();
        if (s.getSelection() != null) s.drawOverlayImage(s.getSelection(), s.getSelX(), s.getSelY());
    }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        if (s.getSelection()==null) return;
        lastX = e.getX(); lastY = e.getY();
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (s.getSelection()==null) return;
        double dx = e.getX()-lastX, dy = e.getY()-lastY;
        s.setSelPos(s.getSelX()+dx, s.getSelY()+dy);
        s.clearOverlay();
        s.drawOverlayImage(s.getSelection(), s.getSelX(), s.getSelY());
        lastX = e.getX(); lastY = e.getY();
    }

    @Override
    public void onDeselect(CanvasState s, HistoryManager h) {
        if (s.getSelection()!=null) {
            s.clearOverlay();
            s.getBase().getGraphicsContext2D().drawImage(s.getSelection(), s.getSelX(), s.getSelY());
            s.setSelection(null);
            h.push();
        }
    }
}
