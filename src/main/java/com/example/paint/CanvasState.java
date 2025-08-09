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

    private final Group content = new Group(base, overlay); // single transform target
    private final Pane container = new Pane(content);
    private final StackPane viewport = new StackPane(container);

    private final HBox statusBar = new HBox();
    private final Label statusLbl = new Label("Ready");
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

    public CanvasState() {
        clearBase(Color.WHITE);

        content.getTransforms().setAll(translate, scale);

        container.setPrefSize(1200, 800);
        container.getStyleClass().add("canvas-container");
        clipToBounds(container);

        viewport.setPadding(new Insets(10));
        statusBar.getChildren().add(statusLbl);
        statusBar.getStyleClass().add("statusbar");

        strokePicker.setOnAction(e -> stroke = strokePicker.getValue());
        fillPicker.setOnAction(e -> fill = fillPicker.getValue());
        brushSlider.valueProperty().addListener((o,a,v)-> brush = v.doubleValue());

        fontFamily.getItems().addAll("Arial","System","Courier New","Times New Roman","Verdana","Consolas");
        fontFamily.setValue("Arial");

        statusLbl.textProperty().bind(statusProp);
        refreshHud();
    }

    // Public UI getters
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

    public WritableImage getSelection(){ return selection; }
    public void setSelection(WritableImage img){ selection = img; }
    public double getSelX(){ return selX; }
    public double getSelY(){ return selY; }
    public void setSelPos(double x, double y){ selX=x; selY=y; }

    public void setStatus(String text){ statusProp.set(text); refreshHud(); }
    public StringProperty statusProperty(){ return statusProp; }

    // View / Pan / Zoom
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

    public void installWheelZoom(Node node) {
        node.addEventFilter(ScrollEvent.SCROLL, ev -> {
            double dy = ev.getDeltaY();
            if (dy == 0) return;
            ev.consume();

            double step = 1.10; // 10% per notch
            if (ev.isShiftDown()) step = Math.pow(step, 1.8);
            if (ev.isControlDown() || ev.isMetaDown()) step = Math.pow(step, 0.35);

            double factor = (dy > 0) ? step : (1.0 / step);
            zoomAtScene(ev.getSceneX(), ev.getSceneY(), factor);
        });
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Resize / Reset
    public void resizeCanvas(double newW, double newH) {
        if (newW <= 1 || newH <= 1) return;
        WritableImage snap = base.snapshot(null, null);

        base.setWidth(newW); base.setHeight(newH);
        overlay.setWidth(newW); overlay.setHeight(newH);

        GraphicsContext g = base.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, newW, newH);
        g.drawImage(snap, 0, 0);

        clearOverlay();
    }

    public void bindToScrollPane(ScrollPane sp) {
        ChangeListener<Bounds> l = (obs, oldV, b) -> resizeCanvas(b.getWidth(), b.getHeight());
        sp.viewportBoundsProperty().addListener(l);
        Bounds b = sp.getViewportBounds();
        if (b != null) resizeCanvas(b.getWidth(), b.getHeight());
    }

    public void resetAll() {
        clearBase(Color.WHITE);
        clearOverlay();
        selection = null;
        selX = selY = 0;
        zoom = 1.0;
        translate.setX(0); translate.setY(0);
        applyPanZoom();
        setStatus("New canvas");
    }

    // Drawing helpers
    public void clearOverlay(){
        overlay.getGraphicsContext2D().clearRect(0,0,overlay.getWidth(), overlay.getHeight());
    }

    public void drawOverlayImage(WritableImage img, double x, double y){
        overlay.getGraphicsContext2D().drawImage(img, x, y);
    }

    public void commitOverlay(){
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage img = overlay.snapshot(sp, null);
        clearOverlay();
        base.getGraphicsContext2D().drawImage(img, 0, 0);
    }

    public void clearBase(Color c){
        GraphicsContext g = base.getGraphicsContext2D();
        g.setFill(c);
        g.fillRect(0,0,base.getWidth(), base.getHeight());
    }

    public void openImage(File f){
        var img = new Image(f.toURI().toString());
        clearBase(Color.WHITE);
        base.getGraphicsContext2D().drawImage(img, 0, 0);
    }

    public void saveImage(File f){
        var snap = base.snapshot(null, null);
        try {
            String name = f.getName().toLowerCase();
            String fmt = name.endsWith(".jpg")||name.endsWith(".jpeg") ? "jpg" : "png";
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
}
