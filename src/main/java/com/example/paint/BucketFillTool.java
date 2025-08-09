package com.example.paint;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Queue;

public class BucketFillTool implements Tool {

    // Safety guard for gigantic canvases
    private static final int MAX_PIXELS = 16_000_000;

    // Tolerance in [0..1], perceptual-ish (0 = strict match). 0.12 works well for anti-aliased lines.
    private static final double DEFAULT_TOLERANCE = 0.12;

    // If you want to expose tolerance later, wire a slider in CanvasState and read it here.
    private double getTolerance(CanvasState s) {
        return DEFAULT_TOLERANCE;
    }

    @Override public String getName(){ return "Bucket"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e) {
        int sx = (int) Math.round(e.getX());
        int sy = (int) Math.round(e.getY());
        floodFillBuffered(s, sx, sy, s.getFill(), getTolerance(s));
        h.push(); // single history entry per fill
    }

    @Override public void onDrag(CanvasState s, HistoryManager h, MouseEvent e) { }
    @Override public void onRelease(CanvasState s, HistoryManager h, MouseEvent e) { }

    /**
     * Flood fill that:
     *  - Snapshots base → int[] buffer
     *  - Runs a tolerant span fill entirely in memory
     *  - Writes back once to the canvas (no mid-draw artifacts)
     */
    private void floodFillBuffered(CanvasState s, int sx, int sy, Color fillColor, double tol) {
        int w = (int) s.getBase().getWidth();
        int h = (int) s.getBase().getHeight();
        if (w <= 0 || h <= 0 || sx < 0 || sy < 0 || sx >= w || sy >= h) return;
        if ((long) w * (long) h > MAX_PIXELS) return;

        // Snapshot with transparent background (preserve alpha)
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage snap = s.getBase().snapshot(sp, null);
        PixelReader pr = snap.getPixelReader();
        if (pr == null) return;

        // Pull pixels into an ARGB buffer
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            pr.getPixels(0, y, w, 1, javafx.scene.image.PixelFormat.getIntArgbInstance(), argb, y * w, w);
        }

        int target = argb[sy * w + sx];
        int replacement = toIntArgb(fillColor);

        // Quick outs
        if (target == replacement) return;

        // If tolerance > 0, we’ll compare with a color distance function
        // Precompute target components
        int ta = (target >>> 24) & 0xFF;
        int tr = (target >>> 16) & 0xFF;
        int tg = (target >>> 8)  & 0xFF;
        int tb = (target)        & 0xFF;

        // Distance threshold in 0..(sqrt(3)*255) mapped from tol 0..1
        double maxDist = tolToDistance(tol);

        boolean[] visited = new boolean[w * h];
        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        visited[sy * w + sx] = true;

        int processed = 0;
        while (!q.isEmpty()) {
            int[] p = q.remove();
            int x = p[0], y = p[1];

            // Expand horizontally as a span
            int lx = x, rx = x;
            int idx;
            // Move left
            while (lx - 1 >= 0 && !visited[y * w + (lx - 1)] && matches(argb[y * w + (lx - 1)], tr, tg, tb, ta, maxDist)) {
                lx--;
                visited[y * w + lx] = true;
            }
            // Move right
            while (rx + 1 < w && !visited[y * w + (rx + 1)] && matches(argb[y * w + (rx + 1)], tr, tg, tb, ta, maxDist)) {
                rx++;
                visited[y * w + rx] = true;
            }

            // Fill the span to replacement
            for (int i = lx; i <= rx; i++) {
                idx = y * w + i;
                argb[idx] = replacement;
                processed++;
                if (processed > MAX_PIXELS) break;
            }
            if (processed > MAX_PIXELS) break;

            // Check the lines above and below the span to enqueue new seeds
            // Above
            if (y - 1 >= 0) {
                int yy = y - 1;
                for (int i = lx; i <= rx; i++) {
                    idx = yy * w + i;
                    if (!visited[idx] && matches(argb[idx], tr, tg, tb, ta, maxDist)) {
                        visited[idx] = true;
                        q.add(new int[]{i, yy});
                    }
                }
            }
            // Below
            if (y + 1 < h) {
                int yy = y + 1;
                for (int i = lx; i <= rx; i++) {
                    idx = yy * w + i;
                    if (!visited[idx] && matches(argb[idx], tr, tg, tb, ta, maxDist)) {
                        visited[idx] = true;
                        q.add(new int[]{i, yy});
                    }
                }
            }
        }

        // Write back to canvas once (no in-progress lines)
        GraphicsContext g = s.getBase().getGraphicsContext2D();
        PixelWriter pw = g.getPixelWriter();
        if (pw == null) return;
        for (int y = 0; y < h; y++) {
            pw.setPixels(0, y, w, 1, javafx.scene.image.PixelFormat.getIntArgbInstance(), argb, y * w, w);
        }
    }

    /** Convert Color → ARGB int (non-premultiplied alpha). */
    private static int toIntArgb(Color c) {
        int a = (int) Math.round(c.getOpacity() * 255.0);
        int r = (int) Math.round(c.getRed()     * 255.0);
        int g = (int) Math.round(c.getGreen()   * 255.0);
        int b = (int) Math.round(c.getBlue()    * 255.0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Compare a pixel to the target with tolerance.
     * We measure Euclidean distance in RGBA (unpremultiplied), weighting alpha lightly so semi-transparent
     * edges are included. If alpha is zero, only alpha is compared to avoid color noise.
     */
    private static boolean matches(int argb, int tr, int tg, int tb, int ta, double maxDist) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b = (argb)        & 0xFF;

        // Exact short-circuit
        if (argb == ((ta << 24) | (tr << 16) | (tg << 8) | tb)) return true;

        // If fully transparent target, match mostly on alpha to avoid picking random colors
        if (ta == 0) {
            return Math.abs(a - ta) <= maxDist;
        }

        // Weighted distance (alpha weight lower so edges join)
        double dr = r - tr;
        double dg = g - tg;
        double db = b - tb;
        double da = (a - ta) * 0.5; // alpha half weight
        double dist = Math.sqrt(dr*dr + dg*dg + db*db + da*da) / 255.0; // normalize ~0..~1.2
        return dist <= maxDist;
    }

    /** Map tolerance [0..1] to a distance threshold. 0.12 ≈ friendly edge capture without leaking. */
    private static double tolToDistance(double tol) {
        // Keep within sane bounds
        tol = Math.max(0.0, Math.min(1.0, tol));
        // Scale slightly sublinear so the low range has finer control
        return Math.pow(tol, 0.8) * 0.9; // 0 → 0, 1 → ~0.9
    }
}
