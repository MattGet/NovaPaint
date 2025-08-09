package com.example.paint;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.Objects;

public class PaintApp extends Application {

    private CanvasState state;
    private HistoryManager history;
    private Tool currentTool;

    // Tools
    private final PencilTool pencil = new PencilTool();
    private final EraserTool eraser = new EraserTool();
    private final LineTool line = new LineTool();
    private final RectTool rect = new RectTool();
    private final EllipseTool ellipse = new EllipseTool();
    private final PolygonTool polygon = new PolygonTool();
    private final SprayTool spray = new SprayTool();
    private final BucketFillTool bucket = new BucketFillTool();
    private final SelectTool select = new SelectTool();
    private final MoveTool move = new MoveTool();
    private final EyedropperTool dropper = new EyedropperTool();
    private final TextTool text = new TextTool();
    private final HandTool hand = new HandTool();

    private boolean darkTheme = true;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        state = new CanvasState();
        history = new HistoryManager(state);
        select.setMoveTool(move);

        HBox appBar = buildAppBar(stage);
        VBox toolPalette = buildToolPalette();
        VBox properties = buildPropertiesPanel();

        ScrollPane scroller = new ScrollPane(state.getViewport());
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        scroller.setPannable(false);
        scroller.setStyle("-fx-background: white; -fx-background-color: white;"); // <— always white
        state.getViewport().setStyle("-fx-background-color: white;");             // <— always white
        state.bindToScrollPane(scroller);

        // Wheel zoom at cursor (stable, single handler)
        state.installWheelZoom(state.getViewport());

        BorderPane root = new BorderPane();
        root.setTop(appBar);
        root.setLeft(toolPalette);
        root.setCenter(scroller);
        root.setRight(properties);
        root.setBottom(buildStatusBar());

        scene = new Scene(root, 1320, 860);
        applyTheme(); // load CSS

        // app icon (optional)
        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))
            ));
        } catch (Exception ignore) {}

        // route drawing events to current tool
        state.getOverlay().setOnMousePressed(e -> { if (currentTool != null) currentTool.onPress(state, history, e); });
        state.getOverlay().setOnMouseDragged(e -> { if (currentTool != null) currentTool.onDrag(state, history, e); });
        state.getOverlay().setOnMouseReleased(e -> { if (currentTool != null) currentTool.onRelease(state, history, e); });

        // hotkeys
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case Z -> { if (e.isControlDown()) history.undo(); }
                case Y -> { if (e.isControlDown()) history.redo(); }
                case ESCAPE -> { if (currentTool != null) currentTool.onCancel(state, history); }
                case ENTER -> { if (currentTool instanceof PolygonCapable p) p.commitPolygon(state, history); }
                case DIGIT1 -> setTool(pencil);
                case DIGIT2 -> setTool(eraser);
                case DIGIT3 -> setTool(line);
                case DIGIT4 -> setTool(rect);
                case DIGIT5 -> setTool(ellipse);
                case DIGIT6 -> setTool(polygon);
                case DIGIT7 -> setTool(spray);
                case DIGIT8 -> setTool(bucket);
                case DIGIT9 -> setTool(select);
                case DIGIT0 -> setTool(move);
            }
        });

        setTool(pencil);

        stage.setTitle("NovaPaint");
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildAppBar(Stage stage) {
        Label title = new Label("NovaPaint");
        title.getStyleClass().add("app-title");

        Button open = topButton(IconFactory.open(), "Open (Ctrl+O)");
        open.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg"));
            var f = fc.showOpenDialog(stage);
            if (f != null) state.openImage(f);
        });

        Button save = topButton(IconFactory.save(), "Save (Ctrl+S)");
        save.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG","*.png"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPEG","*.jpg"));
            var f = fc.showSaveDialog(stage);
            if (f != null) state.saveImage(f);
        });

        Button undo = topButton(IconFactory.undo(), "Undo (Ctrl+Z)");
        undo.setOnAction(e -> history.undo());
        Button redo = topButton(IconFactory.redo(), "Redo (Ctrl+Y)");
        redo.setOnAction(e -> history.redo());

        Button clear = primaryTopButton(IconFactory.clear(), "New Canvas");
        clear.setOnAction(e -> { state.resetAll(); history.clear(); });

        ToggleButton theme = new ToggleButton();
        theme.setGraphic(IconFactory.sunMoon());
        theme.getStyleClass().add("top-icon-button");
        FloatingTooltip.attach(theme, "Toggle Theme");
        theme.setSelected(darkTheme);
        theme.selectedProperty().addListener((obs, o, isDark) -> {
            darkTheme = isDark;
            applyTheme();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, title, spacer, open, save, new Separator(),
                undo, redo, new Separator(), clear, new Separator(), theme);
        bar.getStyleClass().add("appbar");
        bar.setPadding(new Insets(10,12,10,12));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildToolPalette() {
        ToggleGroup group = new ToggleGroup();

        IconToggleButton bPencil  = toolBtn(IconFactory.pencil(), "Pencil (1)", group, () -> setTool(pencil), true);  bPencil.getStyleClass().add("accent-green");
        IconToggleButton bEraser  = toolBtn(IconFactory.eraser(), "Eraser (2)", group, () -> setTool(eraser), false); bEraser.getStyleClass().add("accent-rose");
        IconToggleButton bLine    = toolBtn(IconFactory.line(),   "Line (3)", group, () -> setTool(line),   false);   bLine.getStyleClass().add("accent-cyan");
        IconToggleButton bRect    = toolBtn(IconFactory.rect(),   "Rectangle (4)", group, () -> setTool(rect), false); bRect.getStyleClass().add("accent-indigo");
        IconToggleButton bEllipse = toolBtn(IconFactory.ellipse(),"Ellipse (5)", group, () -> setTool(ellipse), false); bEllipse.getStyleClass().add("accent-purple");
        IconToggleButton bPoly    = toolBtn(IconFactory.polygon(),"Polygon (6)", group, () -> setTool(polygon), false); bPoly.getStyleClass().add("accent-amber");
        IconToggleButton bSpray   = toolBtn(IconFactory.spray(),  "Spray (7)", group, () -> setTool(spray),  false);   bSpray.getStyleClass().add("accent-teal");
        IconToggleButton bBucket  = toolBtn(IconFactory.bucket(), "Bucket (8)", group, () -> setTool(bucket), false);  bBucket.getStyleClass().add("accent-blue");
        IconToggleButton bSelect  = toolBtn(IconFactory.select(), "Select (9)", group, () -> setTool(select), false);  bSelect.getStyleClass().add("accent-orange");
        IconToggleButton bMove    = toolBtn(IconFactory.move(),   "Move (0)", group, () -> setTool(move),    false);   bMove.getStyleClass().add("accent-lime");
        IconToggleButton bPicker  = toolBtn(IconFactory.dropper(),"Eyedropper", group, () -> setTool(dropper), false); bPicker.getStyleClass().add("accent-pink");
        IconToggleButton bText    = toolBtn(IconFactory.text(),   "Text", group, () -> setTool(text), false);          bText.getStyleClass().add("accent-sky");
        IconToggleButton bHand    = toolBtn(IconFactory.hand(),   "Pan (hold SPACE)", group, () -> setTool(hand), false); bHand.getStyleClass().add("accent-slate");

        VBox box = new VBox(6, bPencil,bEraser,bLine,bRect,bEllipse,bPoly,bSpray,bBucket,bSelect,bMove,bPicker,bText,bHand);
        box.getStyleClass().add("tool-rail");
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildPropertiesPanel() {
        // --- Stroke ---
        Label strokeHdr = new Label("Stroke");
        strokeHdr.getStyleClass().add("section");
        var strokePicker = state.getStrokePicker();

        var strokePalette = new ColorPalette("Quick Colors (Stroke)", strokePicker,
                ColorPalette.vibrant24(), 8, 18);

        // --- Fill ---
        Label fillHdr = new Label("Fill");
        fillHdr.getStyleClass().add("section");
        var fillPicker = state.getFillPicker();

        var fillPaletteMain = new ColorPalette("Quick Colors (Fill)", fillPicker,
                ColorPalette.vibrant24(), 8, 18);
        var fillPaletteNeutrals = new ColorPalette("Neutrals", fillPicker,
                ColorPalette.neutrals10(), 10, 16);

        // --- Brush ---
        Label brushHdr = new Label("Brush");
        brushHdr.getStyleClass().add("section");
        var brushRow = new HBox(10, new Label("Size"), state.getBrushSlider());
        brushRow.setAlignment(Pos.CENTER_LEFT);

        // --- Text ---
        Label textHdr = new Label("Text");
        textHdr.getStyleClass().add("section");
        var fontRow1 = new HBox(10, new Label("Family"), state.getFontFamilyBox());
        var fontRow2 = new HBox(10, new Label("Size"), state.getFontSizeSpinner(), state.getBoldCheck(), state.getItalicCheck());
        fontRow1.setAlignment(Pos.CENTER_LEFT);
        fontRow2.setAlignment(Pos.CENTER_LEFT);

        // Wrap sections in “cards”
        VBox strokeCard = card(strokeHdr, strokePicker, strokePalette);
        VBox fillCard   = card(fillHdr, fillPicker, fillPaletteMain, fillPaletteNeutrals);
        VBox brushCard  = card(brushHdr, brushRow);
        VBox textCard   = card(textHdr, fontRow1, fontRow2);

        VBox right = new VBox(16, strokeCard, fillCard, brushCard, textCard);
        right.getStyleClass().add("prop-pane");
        right.setPadding(new Insets(14));
        return right;
    }
    private VBox card(javafx.scene.Node... children) {
        VBox box = new VBox(8, children);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(10));
        return box;
    }

    private HBox buildStatusBar() {
        Label zoomReadout = new Label();
        zoomReadout.textProperty().bind(state.statusProperty());

        HBox bar = new HBox(10, zoomReadout);
        bar.getStyleClass().add("statusbar");
        bar.setPadding(new Insets(6,12,6,12));
        return bar;
    }

    private IconToggleButton toolBtn(javafx.scene.Node icon, String tip, ToggleGroup group, Runnable onSelect, boolean selected) {
        IconToggleButton b = new IconToggleButton(icon);
        FloatingTooltip.attach(b, tip);
        b.setToggleGroup(group);
        b.setSelected(selected);
        b.setOnAction(e -> onSelect.run());
        return b;
    }

    private Button topButton(javafx.scene.Node icon, String tip) {
        Button b = new Button();
        b.getStyleClass().add("top-icon-button");
        b.setGraphic(icon);
        FloatingTooltip.attach(b, tip);
        return b;
    }

    private Button primaryTopButton(javafx.scene.Node icon, String tip) {
        Button b = new Button();
        b.getStyleClass().addAll("top-icon-button","primary");
        b.setGraphic(icon);
        FloatingTooltip.attach(b, tip);
        return b;
    }

    private void setTool(Tool tool) {
        if (currentTool != null) currentTool.onDeselect(state, history);
        currentTool = tool;
        if (currentTool != null) currentTool.onSelect(state, history);
        state.setStatus("Tool: " + tool.getName());
    }

    private void applyTheme() {
        scene.getStylesheets().clear();
        // load the paired theme (dark/light)
        scene.getStylesheets().add(getClass().getResource(darkTheme ? "/css/theme-dark.css" : "/css/theme-light.css").toExternalForm());
    }

    public static void main(String[] args) { launch(args); }
}
