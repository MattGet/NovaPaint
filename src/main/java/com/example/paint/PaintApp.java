package com.example.paint;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.Objects;

public class PaintApp extends Application {

    private CanvasState state;
    private HistoryManager history;

    // Single current tool instance (we reuse tool objects)
    private Tool currentTool;

    // One ToggleGroup for all tool buttons (prevents double-click problem)
    private final ToggleGroup toolGroup = new ToggleGroup();

    // Tools (reused singletons)
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

        HBox appBar = buildAppBar(stage);
        VBox toolPalette = buildToolPalette();       // uses shared toolGroup
        VBox properties = buildPropertiesPanel();

        ScrollPane scroller = new ScrollPane(state.getViewport());
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        scroller.setPannable(false);
        scroller.setStyle("-fx-background: white; -fx-background-color: white;"); // always white around canvas
        state.getViewport().setStyle("-fx-background-color: white;");             // always white around canvas
        state.bindToScrollPane(scroller);

        // Wheel zoom at cursor
        state.installWheelZoom(state.getViewport());

        BorderPane root = new BorderPane();
        root.setTop(appBar);
        root.setLeft(toolPalette);
        root.setCenter(scroller);
        root.setRight(properties);
        root.setBottom(buildStatusBar());

        scene = new Scene(root, 1320, 860);
        applyTheme();

        // --- Robust global accelerators (work regardless of focus) ---
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN),
                () -> { if (currentTool instanceof SelectTool st) st.copySelection(state); }
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN),
                () -> { if (currentTool instanceof SelectTool st) st.cutSelection(state, history); }
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN),
                () -> { if (currentTool instanceof SelectTool st) st.pasteFromClipboard(state, history); }
        );
        // Undo / Redo (Ctrl+Z / Shift+Ctrl+Z / Ctrl+Y)
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN),
                () -> history.undo()
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> history.redo()
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN),
                () -> history.redo()
        );

        // Fallback filter only for Select clipboard ops (optional)
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (!e.isShortcutDown()) return;
            switch (e.getCode()) {
                case C -> { if (currentTool instanceof SelectTool st) { st.copySelection(state); e.consume(); } }
                case X -> { if (currentTool instanceof SelectTool st) { st.cutSelection(state, history); e.consume(); } }
                case V -> { if (currentTool instanceof SelectTool st) { st.pasteFromClipboard(state, history); e.consume(); } }
                default -> {}
            }
        });

        // App icon (optional)
        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))
            ));
        } catch (Exception ignore) {}

        // Route drawing events to current tool (overlay receives input)
        state.getOverlay().setOnMousePressed(e -> { if (currentTool != null) currentTool.onPress(state, history, e); });
        state.getOverlay().setOnMouseDragged(e -> { if (currentTool != null) currentTool.onDrag(state, history, e); });
        state.getOverlay().setOnMouseReleased(e -> { if (currentTool != null) currentTool.onRelease(state, history, e); });

        // Hotkeys that are NOT accelerators (digits for tool switching, Esc/Enter for tooling)
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> { if (currentTool != null) currentTool.onCancel(state, history); }
                case ENTER -> {
                    if (currentTool instanceof PolygonCapable p) p.commitPolygon(state, history);
                }
                case DIGIT1 -> selectToolByShortcut(pencil);
                case DIGIT2 -> selectToolByShortcut(eraser);
                case DIGIT3 -> selectToolByShortcut(line);
                case DIGIT4 -> selectToolByShortcut(rect);
                case DIGIT5 -> selectToolByShortcut(ellipse);
                case DIGIT6 -> selectToolByShortcut(polygon);
                case DIGIT7 -> selectToolByShortcut(spray);
                case DIGIT8 -> selectToolByShortcut(bucket);
                case DIGIT9 -> selectToolByShortcut(select);
                case DIGIT0 -> selectToolByShortcut(move);
                default -> {}
            }
        });

        // ONE listener to handle tool switching for all buttons
        toolGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                toolGroup.selectToggle(oldT);   // prevent no-selection state
                return;
            }
            Tool next = (Tool) newT.getUserData();
            switchTool(next);
        });

        // Select default tool by selecting its toggle (which triggers switchTool)
        selectToggleForTool(pencil);

        // Default fill color to black
        state.getFillPicker().setValue(javafx.scene.paint.Color.BLACK);
        // set default colors
        state.getFillPicker().setValue(javafx.scene.paint.Color.BLACK);
        state.getStrokePicker().setValue(javafx.scene.paint.Color.BLACK);

        // attach the listener first (already in your code), then:
        selectToggleForTool(pencil); // Pencil as true default, fully initialized


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
        IconToggleButton bPencil  = toolBtn(IconFactory.pencil(),  "Pencil (1)",  pencil);  bPencil.getStyleClass().add("accent-green");
        IconToggleButton bEraser  = toolBtn(IconFactory.eraser(),  "Eraser (2)",  eraser);  bEraser.getStyleClass().add("accent-rose");
        IconToggleButton bLine    = toolBtn(IconFactory.line(),    "Line (3)",    line);    bLine.getStyleClass().add("accent-cyan");
        IconToggleButton bRect    = toolBtn(IconFactory.rect(),    "Rectangle (4)", rect);  bRect.getStyleClass().add("accent-indigo");
        IconToggleButton bEllipse = toolBtn(IconFactory.ellipse(), "Ellipse (5)", ellipse); bEllipse.getStyleClass().add("accent-purple");
        IconToggleButton bPoly    = toolBtn(IconFactory.polygon(), "Polygon (6)", polygon); bPoly.getStyleClass().add("accent-amber");
        IconToggleButton bSpray   = toolBtn(IconFactory.spray(),   "Spray (7)",   spray);   bSpray.getStyleClass().add("accent-teal");
        IconToggleButton bBucket  = toolBtn(IconFactory.bucket(),  "Bucket (8)",  bucket);  bBucket.getStyleClass().add("accent-blue");
        IconToggleButton bSelect  = toolBtn(IconFactory.select(),  "Select (9)",  select);  bSelect.getStyleClass().add("accent-orange");
        IconToggleButton bMove    = toolBtn(IconFactory.move(),    "Move (0)",    move);    bMove.getStyleClass().add("accent-lime");
        IconToggleButton bPicker  = toolBtn(IconFactory.dropper(), "Eyedropper",  dropper); bPicker.getStyleClass().add("accent-pink");
        IconToggleButton bText    = toolBtn(IconFactory.text(),    "Text",        text);    bText.getStyleClass().add("accent-sky");
        IconToggleButton bHand    = toolBtn(IconFactory.hand(),    "Pan (hold SPACE)", hand); bHand.getStyleClass().add("accent-slate");


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

    // Create a tool button that participates in the single ToggleGroup flow
    private IconToggleButton toolBtn(javafx.scene.Node icon, String tip, Tool tool) {
        IconToggleButton b = new IconToggleButton(icon);
        FloatingTooltip.attach(b, tip);
        b.setToggleGroup(toolGroup);
        b.setUserData(tool);          // store the tool here
        b.setFocusTraversable(false); // don’t steal keyboard focus
        return b;
    }


    private Button topButton(javafx.scene.Node icon, String tip) {
        Button b = new Button();
        b.getStyleClass().add("top-icon-button");
        b.setGraphic(icon);
        FloatingTooltip.attach(b, tip);
        b.setFocusTraversable(false);
        return b;
    }

    private Button primaryTopButton(javafx.scene.Node icon, String tip) {
        Button b = new Button();
        b.getStyleClass().addAll("top-icon-button","primary");
        b.setGraphic(icon);
        FloatingTooltip.attach(b, tip);
        b.setFocusTraversable(false);
        return b;
    }

    // Centralized switch (called ONLY by toolGroup listener)
    private void switchTool(Tool next) {
        if (next == null || next == currentTool) return;
        try { if (currentTool != null) currentTool.onDeselect(state, history); } catch (Exception ignored) {}
        currentTool = next;
        try { currentTool.onSelect(state, history); } catch (Exception ignored) {}
        state.setStatus("Tool: " + currentTool.getName());
    }

    // Programmatically select a tool via keyboard shortcut (selects the corresponding toggle)
    private void selectToolByShortcut(Tool tool) {
        selectToggleForTool(tool); // triggers switchTool via listener
    }

    private void selectToggleForTool(Tool tool) {
        for (var t : toolGroup.getToggles()) {
            if (t.getUserData() == tool) {
                if (toolGroup.getSelectedToggle() == t) {
                    // If already selected (e.g., preselected earlier), force init
                    switchTool(tool);
                } else {
                    toolGroup.selectToggle(t); // will trigger switchTool via listener
                }
                return;
            }
        }
    }


    private void applyTheme() {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(
                getClass().getResource(darkTheme ? "/css/theme-dark.css" : "/css/theme-light.css").toExternalForm()
        );
    }

    public static void main(String[] args) { launch(args); }
}
