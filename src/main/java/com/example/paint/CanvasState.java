package com.example.paint;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class CanvasState {

    private final Canvas base = new Canvas(1600, 1200);
    private final Canvas overlay = new Canvas(1600, 1200);

    private final Group content = new Group(base, overlay); // transform target
    private final Pane container = new Pane(content);
    private final StackPane viewport = new StackPane(container);

    private final HBox statusBar = new HBox();
    private final StringProperty statusProp = new SimpleStringProperty("Zoom 1.00×   Pan(0, 0)");

    private Color stroke = Color.BLACK;
    private Color fill = Color.TRANSPARENT;
    private double brush = 3;

    private final ColorPicker strokePicker = new ColorPicker(stroke);
    private final ColorPicker fillPicker = new ColorPicker(fill);
    private final Slider brushSlider = new Slider(1, 50, brush);

    private final ComboBox<String> fontFamily = new ComboBox<>();
    private final Spinner<Integer> fontSize = new Spinner<>(8, 128, 20);
    private final CheckBox bold = new CheckBox("B");
    private final CheckBox italic = new CheckBox("I");

    private WritableImage selection;
    private double selX, selY;

    private final Translate translate = new Translate(0, 0);
    private final Scale scale = new Scale(1.0, 1.0, 0, 0);
    private double zoom = 1.0;
    private static final double MIN_ZOOM = 0.1, MAX_ZOOM = 8.0;
    private double lastMouseX = 24, lastMouseY = 24; // sensible default
    private boolean hasMouse = false;

    public CanvasState() {
        // start fully transparent
        clearBaseTransparent();

        content.getTransforms().setAll(translate, scale);

        // Force the area AROUND the canvas to white, regardless of theme
        container.getStyleClass().add("canvas-container");
        container.setPrefSize(1200, 800);
        container.setStyle("-fx-background-color: white;");   // <— always white
        clipToBounds(container);

        viewport.setPadding(new Insets(10));
        viewport.setStyle("-fx-background-color: white;");    // <— always white

        statusBar.getChildren().add(new Label());
        statusBar.getChildren().get(0).styleProperty().set(""); // noop; keep minimal
        refreshHud();

        strokePicker.setOnAction(e -> stroke = strokePicker.getValue());
        fillPicker.setOnAction(e -> fill = fillPicker.getValue());
        brushSlider.valueProperty().addListener((o,a,v)-> brush = v.doubleValue());

        fillPicker.setValue(javafx.scene.paint.Color.BLACK);
        strokePicker.setValue(javafx.scene.paint.Color.BLACK);
        this.fill = fillPicker.getValue();
        this.stroke = strokePicker.getValue();

        // Wire pickers -> internal colors the tools read
        strokePicker.valueProperty().addListener((obs, oldC, newC) -> {
            this.stroke = newC != null ? newC : javafx.scene.paint.Color.BLACK;
            setStatus("Stroke: " + toHex(this.stroke));
        });
        fillPicker.valueProperty().addListener((obs, oldC, newC) -> {
            this.fill = newC != null ? newC : javafx.scene.paint.Color.BLACK;
            setStatus("Fill: " + toHex(this.fill));
        });


        fontFamily.getItems().addAll("Arial","System","Courier New","Times New Roman","Verdana","Consolas");
        fontFamily.setValue("Arial");

        overlay.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            hasMouse = true;
        });
        overlay.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            hasMouse = true;
        });
        // Also update when moving over the base (in case overlay is clear)
        base.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            hasMouse = true;
        });
        base.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            hasMouse = true;
        });
    }

    // ---------- UI getters ----------
    public Canvas getBase() { return base; }
    public Canvas getOverlay() { return overlay; }
    public StackPane getViewport() { return viewport; }
    public HBox getStatusBar() { return statusBar; }

    public ColorPicker getStrokePicker(){ return strokePicker; }
    public ColorPicker getFillPicker(){ return fillPicker; }
    public Slider getBrushSlider(){ return brushSlider; }
    public ComboBox<String> getFontFamilyBox(){ return fontFamily; }
    public Spinner<Integer> getFontSizeSpinner(){ return fontSize; }
    public CheckBox getBoldCheck(){ return bold; }
    public CheckBox getItalicCheck(){ return italic; }

    public Color getStroke() { return stroke; }
    public Color getFill() { return fill; }
    public double getBrush() { return brush; }
    public String getFontFamily(){ return fontFamily.getValue(); }
    public int getFontSize(){ return fontSize.getValue(); }
    public boolean isBold(){ return bold.isSelected(); }
    public boolean isItalic(){ return italic.isSelected(); }
    public double getZoom(){ return zoom; }

    public WritableImage getSelection(){ return selection; }
    public void setSelection(WritableImage img){ selection = img; }
    public double getSelX(){ return selX; }
    public double getSelY(){ return selY; }
    public void setSelPos(double x, double y){ selX=x; selY=y; }
    public double getLastMouseX() { return lastMouseX; }
    public double getLastMouseY() { return lastMouseY; }
    public boolean hasMousePosition() { return hasMouse; }

    public void setStatus(String text){ statusProp.set(text); refreshHud(); }
    public StringProperty statusProperty(){ return statusProp; }

    // ---- Bucket Fill settings ----
    private final javafx.beans.property.DoubleProperty fillTolerance =
            new javafx.beans.property.SimpleDoubleProperty(0.12); // default ~friendly

    private final javafx.beans.property.BooleanProperty fillDiagonalConnectivity =
            new javafx.beans.property.SimpleBooleanProperty(true); // 8-way by default

    private final javafx.beans.property.IntegerProperty fillExpandPixels =
            new javafx.beans.property.SimpleIntegerProperty(1);    // expand 1px default

    public double getFillTolerance() { return fillTolerance.get(); }
    public javafx.beans.property.DoubleProperty fillToleranceProperty() { return fillTolerance; }

    public boolean isFillDiagonalConnectivity() { return fillDiagonalConnectivity.get(); }
    public javafx.beans.property.BooleanProperty fillDiagonalConnectivityProperty() { return fillDiagonalConnectivity; }

    public int getFillExpandPixels() { return fillExpandPixels.get(); }
    public javafx.beans.property.IntegerProperty fillExpandPixelsProperty() { return fillExpandPixels; }


    // ---------- Pan & Zoom ----------
    public void applyPanZoom(){
        scale.setX(zoom);
        scale.setY(zoom);
        refreshHud();
    }

    public void panBy(double dx, double dy){
        translate.setX(translate.getX() + dx);
        translate.setY(translate.getY() + dy);
        applyPanZoom();
    }

    public void panByScene(double sceneDX, double sceneDY){
        Point2D a = container.sceneToLocal(0, 0);
        Point2D b = container.sceneToLocal(sceneDX, sceneDY);
        panBy(b.getX() - a.getX(), b.getY() - a.getY());
    }

    public void zoomAtScene(double sceneX, double sceneY, double factor) {
        double newZoom = clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(newZoom - zoom) < 1e-9) return;

        Point2D pivotInContainer = container.sceneToLocal(sceneX, sceneY);
        Point2D contentPoint = content.sceneToLocal(sceneX, sceneY);

        zoom = newZoom;
        scale.setX(zoom);
        scale.setY(zoom);

        translate.setX(pivotInContainer.getX() - contentPoint.getX() * zoom);
        translate.setY(pivotInContainer.getY() - contentPoint.getY() * zoom);

        applyPanZoom();
    }

    /** Clamp a top-left paste position so the image stays fully on the canvas. */
    public javafx.geometry.Point2D clampPasteTopLeft(double imgW, double imgH, double desiredX, double desiredY) {
        double maxX = Math.max(0, getBase().getWidth()  - imgW);
        double maxY = Math.max(0, getBase().getHeight() - imgH);
        double px = Math.min(Math.max(0, desiredX), maxX);
        double py = Math.min(Math.max(0, desiredY), maxY);
        return new javafx.geometry.Point2D(px, py);
    }

    public void installWheelZoom(Node node) {
        node.addEventFilter(ScrollEvent.SCROLL, ev -> {
            double dy = ev.getDeltaY();
            if (dy == 0) return;
            ev.consume();

            double step = 1.10;
            if (ev.isShiftDown()) step = Math.pow(step, 1.8);
            if (ev.isControlDown() || ev.isMetaDown()) step = Math.pow(step, 0.35);

            double factor = (dy > 0) ? step : (1.0 / step);
            zoomAtScene(ev.getSceneX(), ev.getSceneY(), factor);
        });
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---------- Resize & Reset (transparent-safe) ----------
    public void resizeCanvas(double newW, double newH) {
        if (newW <= 1 || newH <= 1) return;

        // snapshot with transparent fill to preserve alpha
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage snap = base.snapshot(sp, null);

        base.setWidth(newW);
        base.setHeight(newH);
        overlay.setWidth(newW);
        overlay.setHeight(newH);

        // DO NOT paint white — leave transparent background
        GraphicsContext g = base.getGraphicsContext2D();
        g.clearRect(0, 0, newW, newH); // full transparent
        g.drawImage(snap, 0, 0);

        clearOverlay();
    }

    public void bindToScrollPane(ScrollPane sp) {
        // force the scrollpane backgrounds to white as well
        sp.setStyle("-fx-background: white; -fx-background-color: white;");
        ChangeListener<Bounds> l = (obs, oldV, b) -> resizeCanvas(b.getWidth(), b.getHeight());
        sp.viewportBoundsProperty().addListener(l);
        Bounds b = sp.getViewportBounds();
        if (b != null) resizeCanvas(b.getWidth(), b.getHeight());
    }

    public void resetAll() {
        clearBaseTransparent();
        clearOverlay();
        selection = null;
        selX = selY = 0;
        zoom = 1.0;
        translate.setX(0); translate.setY(0);
        applyPanZoom();
        setStatus("New canvas");
    }

    // ---------- Drawing helpers ----------
    public void clearOverlay(){
        overlay.getGraphicsContext2D().clearRect(0,0,overlay.getWidth(), overlay.getHeight());
    }

    /** Draw an image onto the overlay at (x,y). Use for previews/selection ghosts. */
    public void drawOverlayImage(WritableImage img, double x, double y) {
        overlay.getGraphicsContext2D().drawImage(img, x, y);
    }

    /** Merge the current overlay into the base, preserving transparency, then clear the overlay. */
    public void commitOverlay() {
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);                      // keep transparent background
        WritableImage img = overlay.snapshot(sp, null);     // snapshot only what’s on the overlay
        clearOverlay();                                     // wipe overlay after capture
        base.getGraphicsContext2D().drawImage(img, 0, 0);   // composite onto base
    }

    /** Fully transparent clear of the base canvas. */
    public void clearBaseTransparent(){
        base.getGraphicsContext2D().clearRect(0,0,base.getWidth(), base.getHeight());
    }

    /** Kept for compatibility if you ever want a colored fill. */
    public void clearBase(Color c){
        GraphicsContext g = base.getGraphicsContext2D();
        g.setFill(c);
        g.fillRect(0,0,base.getWidth(), base.getHeight());
    }

    public void openImage(File f){
        var img = new Image(f.toURI().toString());
        clearBaseTransparent();                      // keep transparent outside the image
        base.getGraphicsContext2D().drawImage(img, 0, 0);
    }

    public void saveImage(File f){
        // snapshot with TRANSPARENT fill (critical)
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        var snap = base.snapshot(sp, null);

        try {
            String name = f.getName().toLowerCase();
            String fmt = (name.endsWith(".jpg")||name.endsWith(".jpeg")) ? "jpg" : "png";
            // NOTE: JPEG does not support alpha; PNG will preserve transparency.
            ImageIO.write(SwingFXUtils.fromFXImage(snap, null), fmt, f);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void clipToBounds(Region r){
        var rect = new javafx.scene.shape.Rectangle();
        rect.widthProperty().bind(r.widthProperty());
        rect.heightProperty().bind(r.heightProperty());
        r.setClip(rect);
    }

    private void refreshHud() {
        statusProp.set(String.format("Zoom %.2f×   Pan(%.0f, %.0f)", zoom, translate.getX(), translate.getY()));
    }

    private static String toHex(javafx.scene.paint.Color c) {
        int r = (int)Math.round(c.getRed()*255);
        int g = (int)Math.round(c.getGreen()*255);
        int b = (int)Math.round(c.getBlue()*255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
