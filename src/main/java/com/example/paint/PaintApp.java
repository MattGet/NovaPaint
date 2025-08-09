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
    private final PencilTool pencil = new PencilTool(false);
    private final PencilTool eraser = new PencilTool(true);
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

    // theme
    private boolean darkTheme = true;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        state = new CanvasState();
        history = new HistoryManager(state);
        select.setMoveTool(move);

        // --- Top AppBar ---
        HBox appBar = buildAppBar(stage);

        // --- Left Tool Palette ---
        VBox toolPalette = buildToolPalette();

        // --- Right Properties Panel ---
        VBox properties = buildPropertiesPanel();

        // --- Center Canvas (in scrollpane that resizes canvas) ---
        ScrollPane scroller = new ScrollPane(state.getViewport());
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        scroller.setPannable(false);
        state.bindToScrollPane(scroller);

        // Install wheel-zoom ONCE on the viewport (rock-solid pivot)
        state.installWheelZoom(state.getViewport());

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(appBar);
        root.setLeft(toolPalette);
        root.setCenter(scroller);
        root.setRight(properties);
        root.setBottom(buildStatusBar());

        scene = new Scene(root, 1320, 860);
        applyTheme(); // load CSS

        // window icon (optional)
        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))
            ));
        } catch (Exception ignore) {}

        // route drawing events to current tool
        state.getOverlay().setOnMousePressed(e -> { if (currentTool != null) currentTool.onPress(state, history, e); });
        state.getOverlay().setOnMouseDragged(e -> { if (currentTool != null) currentTool.onDrag(state, history, e); });
        state.getOverlay().setOnMouseReleased(e -> { if (currentTool != null) currentTool.onRelease(state, history, e); });

        // keyboard
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

        HBox bar = new HBox(10,
                title, spacer,
                open, save, new Separator(), undo, redo, new Separator(), clear, new Separator(), theme
        );
        bar.getStyleClass().add("appbar");
        bar.setPadding(new Insets(10,12,10,12));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildToolPalette() {
        ToggleGroup group = new ToggleGroup();

        IconToggleButton bPencil  = toolBtn(IconFactory.pencil(), "Pencil (1)", group, () -> setTool(pencil), true);
        IconToggleButton bEraser  = toolBtn(IconFactory.eraser(), "Eraser (2)", group, () -> setTool(eraser), false);
        IconToggleButton bLine    = toolBtn(IconFactory.line(),   "Line (3)", group, () -> setTool(line),   false);
        IconToggleButton bRect    = toolBtn(IconFactory.rect(),   "Rectangle (4)", group, () -> setTool(rect), false);
        IconToggleButton bEllipse = toolBtn(IconFactory.ellipse(),"Ellipse (5)", group, () -> setTool(ellipse), false);
        IconToggleButton bPoly    = toolBtn(IconFactory.polygon(),"Polygon (6)", group, () -> setTool(polygon), false);
        IconToggleButton bSpray   = toolBtn(IconFactory.spray(),  "Spray (7)", group, () -> setTool(spray),  false);
        IconToggleButton bBucket  = toolBtn(IconFactory.bucket(), "Bucket (8)", group, () -> setTool(bucket), false);
        IconToggleButton bSelect  = toolBtn(IconFactory.select(), "Select (9)", group, () -> setTool(select), false);
        IconToggleButton bMove    = toolBtn(IconFactory.move(),   "Move (0)", group, () -> setTool(move),    false);
        IconToggleButton bPicker  = toolBtn(IconFactory.dropper(),"Eyedropper", group, () -> setTool(dropper), false);
        IconToggleButton bText    = toolBtn(IconFactory.text(),   "Text", group, () -> setTool(text), false);
        IconToggleButton bHand    = toolBtn(IconFactory.hand(),   "Pan (hold SPACE)", group, () -> setTool(hand), false);

        VBox box = new VBox(6, bPencil,bEraser,bLine,bRect,bEllipse,bPoly,bSpray,bBucket,bSelect,bMove,bPicker,bText,bHand);
        box.getStyleClass().add("tool-rail");
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildPropertiesPanel() {
        // Stroke & Fill
        Label strokeLbl = new Label("Stroke");
        strokeLbl.getStyleClass().add("section");
        var stroke = state.getStrokePicker();

        Label fillLbl = new Label("Fill");
        fillLbl.getStyleClass().add("section");
        var fill = state.getFillPicker();

        // Brush
        Label brushLbl = new Label("Brush");
        brushLbl.getStyleClass().add("section");
        var brushRow = new HBox(10, new Label("Size"), state.getBrushSlider());
        brushRow.setAlignment(Pos.CENTER_LEFT);

        // Font
        Label fontLbl = new Label("Text");
        fontLbl.getStyleClass().add("section");
        var fontRow1 = new HBox(10, new Label("Family"), state.getFontFamilyBox());
        var fontRow2 = new HBox(10, new Label("Size"), state.getFontSizeSpinner(), state.getBoldCheck(), state.getItalicCheck());
        fontRow1.setAlignment(Pos.CENTER_LEFT);
        fontRow2.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(16,
                strokeLbl, stroke,
                fillLbl, fill,
                brushLbl, brushRow,
                fontLbl, fontRow1, fontRow2
        );
        box.getStyleClass().add("prop-pane");
        box.setPadding(new Insets(14));
        return box;
    }

    private HBox buildStatusBar() {
        Label zoom = new Label();
        zoom.textProperty().bind(state.statusProperty()); // reflect "zoom | pan" text

        HBox bar = new HBox(10, zoom);
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
        scene.getStylesheets().add(getClass().getResource(darkTheme ? "/css/theme-dark.css" : "/css/theme-light.css").toExternalForm());
    }

    public static void main(String[] args) { launch(args); }
}
