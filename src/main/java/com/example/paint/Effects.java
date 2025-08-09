package com.example.paint;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class Effects {
    public static void applyGray(CanvasState s, HistoryManager h){
        h.push();
        bakeAdjust(s, ca -> ca.setSaturation(-1));
    }
    public static void applyBrightness(CanvasState s, HistoryManager h, double delta){
        h.push();
        bakeAdjust(s, ca -> ca.setBrightness(Math.max(-1, Math.min(1, ca.getBrightness()+delta))));
    }
    public static void applyInvert(CanvasState s, HistoryManager h){
        h.push();
        var img = s.getBase().snapshot(null,null);
        var out = new WritableImage((int)img.getWidth(), (int)img.getHeight());
        PixelReader pr = img.getPixelReader();
        PixelWriter pw = out.getPixelWriter();
        for (int y=0;y<img.getHeight();y++){
            for (int x=0;x<img.getWidth();x++){
                Color c = pr.getColor(x,y);
                pw.setColor(x,y, new Color(1-c.getRed(), 1-c.getGreen(), 1-c.getBlue(), c.getOpacity()));
            }
        }
        s.getBase().getGraphicsContext2D().drawImage(out, 0, 0);
    }

    private interface CA { void apply(ColorAdjust ca); }
    private static void bakeAdjust(CanvasState s, CA fn){
        ColorAdjust ca = new ColorAdjust(); fn.apply(ca);
        var snap = s.getBase().snapshot(null, null);
        var iv = new ImageView(snap); iv.setEffect(ca);
        var baked = iv.snapshot(null, null);
        s.getBase().getGraphicsContext2D().drawImage(baked, 0, 0);
    }
}
