package com.example.paint;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Queue;

public class BucketFillTool implements Tool {

    private static final int MAX_PIXELS = 16_000_000;

    @Override public String getName(){ return "Bucket"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        int sx = (int) Math.floor(e.getX());
        int sy = (int) Math.floor(e.getY());
        // Fallback if fill picker hasn’t initialized yet
        Color fill = s.getFill() != null ? s.getFill() : Color.BLACK;

        double tol = s.getFillTolerance();            // 0..1 (UI)
        boolean diag = s.isFillDiagonalConnectivity(); // 4-way vs 8-way
        int expand = Math.max(0, Math.min(3, s.getFillExpandPixels()));

        floodFillBuffered(s, sx, sy, fill, tol, diag, expand);
        h.push(); // one history entry
    }

    @Override public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) { }
    @Override public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) { }

    private void floodFillBuffered(CanvasState s, int sx, int sy, Color fillColor,
                                   double tol, boolean diagonal, int expandPixels) {

        int w = (int) s.getBase().getWidth();
        int h = (int) s.getBase().getHeight();
        if (w <= 0 || h <= 0 || sx < 0 || sy < 0 || sx >= w || sy >= h) return;
        if ((long) w * (long) h > MAX_PIXELS) return;

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage snap = s.getBase().snapshot(sp, null);
        PixelReader pr = snap.getPixelReader();
        if (pr == null) return;

        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            pr.getPixels(0, y, w, 1, PixelFormat.getIntArgbInstance(), argb, y * w, w);
        }

        int startIdx = sy * w + sx;
        int target = argb[startIdx];
        int replacement = toIntArgb(fillColor);

        // If the region is already exactly the replacement color and tol is ~0, nothing to do
        if (target == replacement && tol <= 1e-6) return;

        // Prep target components and distance threshold
        int ta = (target >>> 24) & 0xFF;
        int tr = (target >>> 16) & 0xFF;
        int tg = (target >>> 8)  & 0xFF;
        int tb = (target)        & 0xFF;
        double maxDist = tolToDistance(tol);

        // Build a mask of which pixels belong to the region
        boolean[] inRegion = new boolean[w * h];
        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        inRegion[startIdx] = true;

        int[][] neighbors4 = {{1,0},{-1,0},{0,1},{0,-1}};
        int[][] neighbors8 = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        int[][] N = diagonal ? neighbors8 : neighbors4;

        while (!q.isEmpty()) {
            int[] p = q.remove();
            int x = p[0], y = p[1];

            for (int[] d : N) {
                int nx = x + d[0], ny = y + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int ni = ny * w + nx;
                if (inRegion[ni]) continue;

                int c = argb[ni];
                if (matches(c, tr, tg, tb, ta, maxDist)) {
                    inRegion[ni] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }

        // Optional: expand (dilate) mask by N pixels to hug anti-aliased borders
        for (int k = 0; k < expandPixels; k++) {
            boolean[] next = inRegion.clone();
            for (int y = 0; y < h; y++) {
                int row = y * w;
                for (int x = 0; x < w; x++) {
                    int i = row + x;
                    if (inRegion[i]) continue;
                    boolean any = false;
                    for (int[] d : N) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (inRegion[ny * w + nx]) { any = true; break; }
                    }
                    if (any) next[i] = true;
                }
            }
            inRegion = next;
        }

        // Paint: write back exactly once (no mid-fill streaks)
        for (int i = 0; i < argb.length; i++) {
            if (inRegion[i]) argb[i] = replacement;
        }
        GraphicsContext g = s.getBase().getGraphicsContext2D();
        PixelWriter pw = g.getPixelWriter();
        for (int y = 0; y < h; y++) {
            pw.setPixels(0, y, w, 1, PixelFormat.getIntArgbInstance(), argb, y * w, w);
        }
    }

    private static int toIntArgb(Color c) {
        int a = (int) Math.round(c.getOpacity() * 255.0);
        int r = (int) Math.round(c.getRed()     * 255.0);
        int g = (int) Math.round(c.getGreen()   * 255.0);
        int b = (int) Math.round(c.getBlue()    * 255.0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static boolean matches(int argb, int tr, int tg, int tb, int ta, double maxDist) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b = (argb)        & 0xFF;

        if (argb == ((ta << 24) | (tr << 16) | (tg << 8) | tb)) return true;
        if (ta == 0) { // fully transparent target: compare mostly alpha
            return Math.abs(a - ta) <= maxDist * 255.0;
        }
        double dr = r - tr, dg = g - tg, db = b - tb, da = (a - ta) * 0.5; // alpha half-weight
        double dist = Math.sqrt(dr*dr + dg*dg + db*db + da*da) / 255.0;    // normalized ~0..1.2
        return dist <= maxDist;
    }

    private static double tolToDistance(double tol) {
        tol = Math.max(0.0, Math.min(1.0, tol));
        return Math.pow(tol, 0.8) * 0.9;
    }
}
