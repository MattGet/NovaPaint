package com.example.paint;

import javafx.scene.input.MouseEvent;

public class SprayTool implements Tool {
    @Override public String getName(){ return "Spray"; }

    private void spray(CanvasState s, double x, double y){
        var g = s.getBase().getGraphicsContext2D();
        g.setFill(s.getStroke());
        int dots = (int)(s.getBrush()*8);
        double r = s.getBrush()*1.5;
        for (int i=0;i<dots;i++){
            double a = Math.random()*Math.PI*2;
            double d = Math.random()*r;
            g.fillOval(x + Math.cos(a)*d, y + Math.sin(a)*d, 1,1);
        }
    }

    @Override public void onPress(CanvasState s, HistoryManager h, MouseEvent e){ h.push(); spray(s, e.getX(), e.getY()); }
    @Override public void onDrag(CanvasState s, HistoryManager h, MouseEvent e){ spray(s, e.getX(), e.getY()); }
}
