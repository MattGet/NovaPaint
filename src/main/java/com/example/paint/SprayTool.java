package com.example.paint;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

import java.util.concurrent.ThreadLocalRandom;

public class SprayTool implements Tool {
    private boolean spraying;
    private static final int PARTICLES = 30;

    @Override public String getName(){ return "Spray"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        spraying = true;
        sprayAt(s.getBase().getGraphicsContext2D(), s, e.getX(), e.getY());
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!spraying) return;
        sprayAt(s.getBase().getGraphicsContext2D(), s, e.getX(), e.getY());
    }

    @Override
    public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) {
        if (!spraying) return;
        spraying = false;
        h.push(); // one history step per spray gesture
    }

    private void sprayAt(GraphicsContext g, CanvasState s, double x, double y) {
        g.setFill(s.getStroke());
        double r = s.getBrush() * 0.8;
        var rnd = ThreadLocalRandom.current();
        for (int i=0; i<PARTICLES; i++) {
            double a = rnd.nextDouble(0, Math.PI*2);
            double d = rnd.nextDouble(0, r);
            double px = x + Math.cos(a)*d;
            double py = y + Math.sin(a)*d;
            g.fillRect(px, py, 1, 1);
        }
    }
}
