package com.example.paint;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;

public class BucketFillTool implements Tool {
    @Override public String getName(){ return "Bucket"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        h.push();
        floodFill(s, (int)e.getX(), (int)e.getY(), s.getStroke());
    }

    private void floodFill(CanvasState s, int x0, int y0, Color color){
        var img = s.getBase().snapshot(null, null);
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        if (x0<0||y0<0||x0>=w||y0>=h) return;

        PixelReader pr = img.getPixelReader();
        int target = ColorUtils.argb(pr.getColor(x0,y0));
        int repl = ColorUtils.argb(color);
        if (target == repl) return;

        int[] data = new int[w*h];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) data[y*w+x] = ColorUtils.argb(pr.getColor(x,y));

        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{x0,y0});
        while(!q.isEmpty()){
            var p = q.removeFirst();
            int x=p[0], y=p[1];
            if (x<0||y<0||x>=w||y>=h) continue;
            int idx=y*w+x; if (data[idx]!=target) continue;
            data[idx] = repl;
            q.add(new int[]{x+1,y}); q.add(new int[]{x-1,y}); q.add(new int[]{x,y+1}); q.add(new int[]{x,y-1});
        }

        WritableImage out = new WritableImage(w,h);
        PixelWriter pw = out.getPixelWriter();
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) pw.setArgb(x,y, data[y*w+x]);

        s.getBase().getGraphicsContext2D().drawImage(out, 0, 0);
    }
}
