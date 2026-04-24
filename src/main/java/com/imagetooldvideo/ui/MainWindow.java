package com.imagetooldvideo.ui;

import com.imagetooldvideo.model.EffectConfig;
import com.imagetooldvideo.model.EffectMode;
import com.imagetooldvideo.model.GalleryEntry;
import com.imagetooldvideo.model.GalleryManager;
import com.imagetooldvideo.model.GallerySettings;
import com.imagetooldvideo.model.Preset;
import com.imagetooldvideo.model.PresetManager;
import com.imagetooldvideo.model.VideoEntry;
import com.imagetooldvideo.model.VideoManager;
import com.imagetooldvideo.model.VideoSettings;
import com.imagetooldvideo.processing.ImageProcessor;
import com.imagetooldvideo.processing.VideoCreator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.prefs.Preferences;

public class MainWindow extends Application {

    private static final double WINDOW_WIDTH = 1260;
    private static final double WINDOW_HEIGHT = 920;
    private static final double MIN_TOP_PANEL_HEIGHT = 200;
    private static final double MIN_MIDDLE_PANEL_HEIGHT = 220;
    private static final double DEFAULT_DIVIDER_POSITION = 0.6;
    private static final String PREF_DIVIDER_POSITION = "ui.mainDividerPosition";
    private static final int PREVIEW_MIN_SECONDS = 1;
    private static final int PREVIEW_MAX_SECONDS = 10;
    private static final Insets COMPACT_INSETS = new Insets(5);
    private static final String BUTTON_BASE_STYLE = "-fx-background-color: #444444; -fx-text-fill: #cccccc; -fx-padding: 1 2 1 2; -fx-font-size: 11px;";
    private static final String TOGGLE_OFF_STYLE = "-fx-background-color: #555555; -fx-text-fill: #dddddd; -fx-padding: 1 6 1 6; -fx-font-size: 10px;";
    private static final String TOGGLE_ON_STYLE = "-fx-background-color: #2f8f46; -fx-text-fill: white; -fx-padding: 1 6 1 6; -fx-font-size: 10px;";

    // ── State ────────────────────────────────────────────────────────────────
    private final ImageProcessor imageProcessor = new ImageProcessor();
    private final VideoCreator videoCreator = new VideoCreator();
    private final VideoSettings settings = new VideoSettings();

    private BufferedImage processedImage;
    private BufferedImage rawEvenImage;

    // ── UI nodes ─────────────────────────────────────────────────────────────
    private GalleryManager galleryManager;
    private ListView<GalleryEntry> galleryList;
    private final Map<String, Image> thumbnailCache = new HashMap<>();

    private VideoManager videoManager;
    private GallerySettings gallerySettings;
    private ListView<VideoEntry> videoList;
    private ProgressBar storageBar;
    private Label storageLabel;

    private Label previewTitle;
    private ImageView previewImageView;
    private Slider previewFrameSlider;
    private Label previewFrameLabel;
    private Spinner<Integer> previewSecondsSpinner;
    private ComboBox<Integer> previewFpsCombo;
    private CheckBox previewLoopCheck;
    private CheckBox effectsOnlyPreviewCheck;
    private Button previewPlayButton;
    private ComboBox<Preset> presetCombo;
    private ComboBox<String> customPresetCombo;
    private Spinner<Integer> durationSpinner;
    private TextField outputDirField;
    private Button exportButton;
    private Button exportEffectsButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Path lastExportedPath;
    private File currentImageFile;

    private EffectControls lightLeakControls;
    private EffectControls vignetteControls;
    private EffectControls grainControls;
    private EffectControls dustControls;
    private EffectControls scratchesControls;
    private EffectControls trackingHControls;
    private EffectControls trackingVControls;
    private EffectControls flickerControls;

    private boolean applyingPresetDefaults;
    private PresetManager presetManager;
    private int previewFrameIndex = 0;
    private Timeline previewTimeline;
    private final Preferences uiPrefs = Preferences.userNodeForPackage(MainWindow.class);

    private static final class EffectControls {
        private final ToggleButton modeToggle;
        private final Slider variance;
        private final Label varianceValue;
        private final Slider frequency;
        private final Label frequencyValue;
        private final Slider randomness;
        private final Label randomnessValue;
        private final Canvas waveform;

        private EffectControls(
                ToggleButton modeToggle,
                Slider variance,
                Label varianceValue,
                Slider frequency,
                Label frequencyValue,
                Slider randomness,
                Label randomnessValue,
                Canvas waveform) {
            this.modeToggle = modeToggle;
            this.variance = variance;
            this.varianceValue = varianceValue;
            this.frequency = frequency;
            this.frequencyValue = frequencyValue;
            this.randomness = randomness;
            this.randomnessValue = randomnessValue;
            this.waveform = waveform;
        }
    }

    // ── Application entry ────────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        stage.setTitle("Image to Old Video  —  v0.1.0");
        presetManager = new PresetManager(Path.of(System.getProperty("user.home"), ".image-to-old-video"));

        BorderPane root = new BorderPane();
        root.setPadding(COMPACT_INSETS);
        root.setStyle("-fx-background-color: #1e1e1e;");

        root.setTop(buildTopBar());

        HBox topPanel = buildCenter();
        topPanel.setMinHeight(MIN_TOP_PANEL_HEIGHT);
        VBox middlePanel = buildPresetAndEffectsPanel(stage);
        middlePanel.setMinHeight(MIN_MIDDLE_PANEL_HEIGHT);

        SplitPane verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().addAll(topPanel, middlePanel);
        verticalSplit.setDividerPositions(DEFAULT_DIVIDER_POSITION);
        SplitPane.setResizableWithParent(topPanel, true);
        SplitPane.setResizableWithParent(middlePanel, true);
        bindDividerPersistence(verticalSplit);

        root.setCenter(verticalSplit);
        root.setBottom(buildBottomBars());

        // Sync button state after all UI nodes exist
        updateStorageIndicator();
        updateExportButtonState();

        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        stage.setResizable(false);
        stage.show();

        double savedDivider = uiPrefs.getDouble(PREF_DIVIDER_POSITION, DEFAULT_DIVIDER_POSITION);
        Platform.runLater(() -> verticalSplit.setDividerPositions(savedDivider));

        loadSampleImageIfNeeded();
    }

    private void bindDividerPersistence(SplitPane splitPane) {
        if (splitPane.getDividers().isEmpty()) {
            return;
        }
        ReadOnlyDoubleProperty pos = splitPane.getDividers().get(0).positionProperty();
        pos.addListener((obs, oldVal, newVal) -> uiPrefs.putDouble(PREF_DIVIDER_POSITION, newVal.doubleValue()));
    }

    /** Loads the bundled sample image on first launch so the UI is not empty. */
    private void loadSampleImageIfNeeded() {
        if (rawEvenImage != null) {
            return; // user already loaded something
        }
        // Resolve relative to the JAR / working directory so it works both in
        // development (mvn javafx:run from the project root) and when packaged.
        File sample = new File("images/sample.jpg");
        if (!sample.exists()) {
            // Try one level up in case the working directory is the project parent
            sample = new File("image-to-old-video/images/sample.jpg");
        }
        if (sample.exists()) {
            loadImageFile(sample);
        }
    }

    // ── Top bar ──────────────────────────────────────────────────────────────
    private HBox buildTopBar() {
        Button helpButton = new Button("Help");
        helpButton.setStyle(BUTTON_BASE_STYLE);
        helpButton.setOnAction(e -> {
            Alert help = new Alert(Alert.AlertType.INFORMATION);
            help.setTitle("Help");
            help.setHeaderText("Image to Old Video v0.1.0");
            help.setContentText("1) Load an image in the top section.\n"
                    + "2) Pick a preset and adjust effects.\n"
                    + "3) Click Export Video at the bottom.");
            help.showAndWait();
        });

        Label title = new Label("Image to Old Video");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #d4a847;");
        Label version = new Label("v0.1.0");
        version.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        VBox titleBox = new VBox(1, title, version);
        titleBox.setAlignment(Pos.CENTER);

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox bar = new HBox(8, helpButton, leftSpacer, titleBox, rightSpacer);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 5, 0));
        return bar;
    }

    // ── Center: left tab pane (images / videos) + preview ───────────────────────
    private HBox buildCenter() {
        HBox center = new HBox(8);
        center.setAlignment(Pos.TOP_LEFT);

        TabPane leftTabs = buildLeftTabPane();
        VBox previewBox  = buildPreviewBox();
        HBox.setHgrow(leftTabs,   Priority.ALWAYS);
        HBox.setHgrow(previewBox, Priority.ALWAYS);

        center.getChildren().addAll(leftTabs, previewBox);
        return center;
    }

    private TabPane buildLeftTabPane() {
        gallerySettings = new GallerySettings();
        videoManager    = new VideoManager();

        Label imgLabel = new Label("🖼️  Images");
        imgLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        Tab imagesTab = new Tab();
        imagesTab.setGraphic(imgLabel);
        imagesTab.setContent(buildGalleryPanel());
        imagesTab.setClosable(false);

        Label vidLabel = new Label("🎬  Videos");
        vidLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        Tab videosTab = new Tab();
        videosTab.setGraphic(vidLabel);
        videosTab.setContent(buildVideosPanel());
        videosTab.setClosable(false);

        TabPane tabs = new TabPane(imagesTab, videosTab);
        tabs.setPrefWidth(450);
        tabs.setMinWidth(380);
        tabs.setStyle("-fx-background-color: #1e1e1e;");
        return tabs;
    }

    private VBox buildGalleryPanel() {
        galleryManager = new GalleryManager();

        // ── Header row ────────────────────────────────────────────────────────
        Label titleLabel = new Label("Image Gallery");
        titleLabel.setStyle("-fx-text-fill: #d4a847; -fx-font-size: 13px; -fx-font-weight: bold;");

        galleryList = new ListView<>();
        galleryList.getItems().addAll(galleryManager.getEntries());

        Button addButton = new Button("Add\u2026");
        addButton.setStyle(BUTTON_BASE_STYLE);
        addButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Add Images to Gallery");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
            List<File> files = chooser.showOpenMultipleDialog(galleryList.getScene().getWindow());
            if (files != null && !files.isEmpty()) {
                for (File f : files) {
                    galleryManager.addEntry(f.getAbsolutePath(), stripExtension(f.getName()));
                }
                refreshGalleryList();
                loadImageFile(files.get(0));
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(5, titleLabel, spacer, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── List ──────────────────────────────────────────────────────────────
        Label emptyLabel = new Label("No images yet.\nDrag image files here to add them.");
        emptyLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px; -fx-text-alignment: center;");
        galleryList.setPlaceholder(emptyLabel);
        galleryList.setCellFactory(lv -> createGalleryCell());
        galleryList.setStyle("-fx-background-color: #232323; -fx-border-color: #3a3a3a; -fx-border-width: 1;");
        VBox.setVgrow(galleryList, Priority.ALWAYS);

        galleryList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                GalleryEntry selected = galleryList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadImageFile(new File(selected.path()));
                }
            }
        });

        // ── Panel ─────────────────────────────────────────────────────────────
        VBox panel = new VBox(5, header, galleryList);
        panel.setPrefSize(420, 360);
        panel.setPadding(COMPACT_INSETS);
        panel.setBorder(new Border(new BorderStroke(
                Color.web("#555555"), BorderStrokeStyle.DASHED,
                new CornerRadii(8), new BorderWidths(2))));
        panel.setStyle("-fx-background-color: #2a2a2a;");

        panel.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        panel.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                for (File f : db.getFiles()) {
                    galleryManager.addEntry(f.getAbsolutePath(), stripExtension(f.getName()));
                }
                refreshGalleryList();
                loadImageFile(db.getFiles().get(0));
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        return panel;
    }

    private ListCell<GalleryEntry> createGalleryCell() {
        ImageView thumb = new ImageView();
        thumb.setFitWidth(64);
        thumb.setFitHeight(44);
        thumb.setPreserveRatio(true);

        StackPane thumbPane = new StackPane(thumb);
        thumbPane.setMinSize(64, 44);
        thumbPane.setPrefSize(64, 44);
        thumbPane.setStyle("-fx-background-color: #111111;");

        Label nameLabel = new Label();
        nameLabel.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 12px; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(260);

        Label descLabel = new Label();
        descLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(260);

        VBox textBox = new VBox(2, nameLabel, descLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(10, thumbPane, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(COMPACT_INSETS);

        MenuItem renameItem  = new MenuItem("Rename\u2026");
        MenuItem descItem    = new MenuItem("Edit Description\u2026");
        MenuItem removeItem  = new MenuItem("Remove from Gallery");
        ContextMenu ctx = new ContextMenu(renameItem, descItem, new SeparatorMenuItem(), removeItem);

        ListCell<GalleryEntry> cell = new ListCell<>() {
            @Override
            protected void updateItem(GalleryEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    nameLabel.setText(entry.name());
                    String desc = entry.description();
                    descLabel.setText(desc);
                    descLabel.setVisible(!desc.isBlank());
                    descLabel.setManaged(!desc.isBlank());
                    thumb.setImage(loadThumbnail(entry.path()));
                    setGraphic(row);
                    setStyle("-fx-background-color: #2a2a2a;");
                }
            }
        };
        cell.setContextMenu(ctx);

        renameItem.setOnAction(e -> {
            GalleryEntry entry = cell.getItem();
            if (entry == null) return;
            TextInputDialog dlg = new TextInputDialog(entry.name());
            dlg.setTitle("Rename Image");
            dlg.setHeaderText(null);
            dlg.setContentText("New name:");
            dlg.showAndWait().ifPresent(n -> {
                if (!n.isBlank()) {
                    galleryManager.updateEntry(entry.withName(n.trim()));
                    refreshGalleryList();
                }
            });
        });
        descItem.setOnAction(e -> {
            GalleryEntry entry = cell.getItem();
            if (entry == null) return;
            TextInputDialog dlg = new TextInputDialog(entry.description());
            dlg.setTitle("Edit Description");
            dlg.setHeaderText(null);
            dlg.setContentText("Description:");
            dlg.showAndWait().ifPresent(d ->
                    galleryManager.updateEntry(entry.withDescription(d.trim())));
            refreshGalleryList();
        });
        removeItem.setOnAction(e -> {
            GalleryEntry entry = cell.getItem();
            if (entry == null) return;
            galleryManager.removeEntry(entry.id());
            refreshGalleryList();
        });

        return cell;
    }

    private Image loadThumbnail(String path) {
        if (thumbnailCache.containsKey(path)) return thumbnailCache.get(path);
        try {
            BufferedImage img = ImageIO.read(new File(path));
            if (img == null) return null;
            BufferedImage scaled = new BufferedImage(64, 44, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, 0, 0, 64, 44, null);
            g2.dispose();
            Image fxImg = SwingFXUtils.toFXImage(scaled, null);
            thumbnailCache.put(path, fxImg);
            return fxImg;
        } catch (Exception ex) {
            return null;
        }
    }

    private void refreshGalleryList() {
        GalleryEntry selected = galleryList.getSelectionModel().getSelectedItem();
        galleryList.getItems().setAll(galleryManager.getEntries());
        if (selected != null) {
            final String id = selected.id();
            galleryList.getItems().stream()
                    .filter(e -> e.id().equals(id))
                    .findFirst()
                    .ifPresent(e -> galleryList.getSelectionModel().select(e));
        }
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    // ── Videos panel ────────────────────────────────────────────────────────────
    private VBox buildVideosPanel() {
        // ── Storage indicator ──────────────────────────────────────────
        storageBar = new ProgressBar(0);
        storageBar.setPrefWidth(Double.MAX_VALUE);
        storageBar.setMaxWidth(Double.MAX_VALUE);
        storageBar.setPrefHeight(12);

        storageLabel = new Label();
        storageLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

        VBox storageBox = new VBox(3, storageBar, storageLabel);
        HBox.setHgrow(storageBox, Priority.ALWAYS);

        Button settingsBtn = new Button("⚙ Settings");
        settingsBtn.setStyle(BUTTON_BASE_STYLE);
        settingsBtn.setOnAction(e -> showGallerySettingsDialog());

        HBox header = new HBox(5, storageBox, settingsBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 5, 0));

        // ── Video list ─────────────────────────────────────────────
        Label emptyLabel = new Label("No videos yet.\nExport a video to see it here.");
        emptyLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px; -fx-text-alignment: center;");

        videoList = new ListView<>();
        videoList.getItems().addAll(videoManager.getEntries());
        videoList.setPlaceholder(emptyLabel);
        videoList.setCellFactory(lv -> createVideoCell());
        videoList.setStyle("-fx-background-color: #232323; -fx-border-color: #3a3a3a; -fx-border-width: 1;");
        VBox.setVgrow(videoList, Priority.ALWAYS);

        // Double-click to play
        videoList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                VideoEntry sel = videoList.getSelectionModel().getSelectedItem();
                if (sel != null) openVideoInDefaultPlayer(Path.of(sel.path()));
            }
        });

        // ── Panel container ─────────────────────────────────────────
        VBox panel = new VBox(5, header, videoList);
        panel.setPadding(COMPACT_INSETS);
        panel.setBorder(new Border(new BorderStroke(
                Color.web("#555555"), BorderStrokeStyle.DASHED,
                new CornerRadii(8), new BorderWidths(2))));
        panel.setStyle("-fx-background-color: #2a2a2a;");

        return panel;
    }

    private ListCell<VideoEntry> createVideoCell() {
        // Thumbnail area (80x45) with play-button overlay
        ImageView thumbView = new ImageView();
        thumbView.setFitWidth(80);
        thumbView.setFitHeight(45);
        thumbView.setPreserveRatio(true);
        thumbView.setSmooth(true);

        Label playOverlay = new Label("▶");
        playOverlay.setStyle("-fx-text-fill: white; -fx-font-size: 20px;"
                + " -fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");

        StackPane thumbStack = new StackPane(thumbView, playOverlay);
        thumbStack.setMinWidth(80);
        thumbStack.setMaxWidth(80);
        thumbStack.setMinHeight(45);
        thumbStack.setMaxHeight(45);
        thumbStack.setStyle("-fx-background-color: #111111;");
        StackPane.setAlignment(playOverlay, Pos.CENTER);

        Label nameLabel = new Label();
        nameLabel.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 12px; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(270);

        Label infoLabel = new Label();
        infoLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px;");

        VBox textBox = new VBox(2, nameLabel, infoLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(10, thumbStack, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(COMPACT_INSETS);

        MenuItem playItem   = new MenuItem("▶  Play");
        MenuItem renameItem = new MenuItem("Rename…");
        MenuItem revealItem = new MenuItem("Show in Explorer");
        MenuItem deleteItem = new MenuItem("Delete from Disk");
        ContextMenu ctx = new ContextMenu(playItem, renameItem, revealItem,
                new SeparatorMenuItem(), deleteItem);

        ListCell<VideoEntry> cell = new ListCell<>() {
            @Override
            protected void updateItem(VideoEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    nameLabel.setText(entry.name());
                    infoLabel.setText(formatBytes(entry.sizeBytes())
                            + "  •  " + formatDate(entry.createdAt()));
                    // Load source-image thumbnail
                    String src = entry.sourceImagePath();
                    Image thumb = null;
                    if (src != null && !src.isBlank()) {
                        thumb = thumbnailCache.computeIfAbsent("vid:" + src, k -> {
                            try {
                                return new Image(new File(src).toURI().toString(),
                                        80, 45, true, true, false);
                            } catch (Exception ex) { return null; }
                        });
                    }
                    thumbView.setImage(thumb);
                    setGraphic(row);
                    setStyle("-fx-background-color: #2a2a2a;");
                }
            }
        };
        cell.setContextMenu(ctx);

        // Drag-out to Explorer / Finder
        cell.setOnDragDetected(e -> {
            VideoEntry entry = cell.getItem();
            if (entry == null) return;
            File f = new File(entry.path());
            if (!f.exists()) return;
            Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putFiles(List.of(f));
            db.setContent(content);
            e.consume();
        });

        playItem.setOnAction(e -> {
            VideoEntry entry = cell.getItem();
            if (entry != null) openVideoInDefaultPlayer(Path.of(entry.path()));
        });
        renameItem.setOnAction(e -> {
            VideoEntry entry = cell.getItem();
            if (entry == null) return;
            TextInputDialog dlg = new TextInputDialog(entry.name());
            dlg.setTitle("Rename Video");
            dlg.setHeaderText(null);
            dlg.setContentText("New name:");
            dlg.showAndWait().ifPresent(n -> {
                if (!n.isBlank()) {
                    videoManager.updateEntry(entry.withName(n.trim()));
                    refreshVideoList();
                }
            });
        });
        revealItem.setOnAction(e -> {
            VideoEntry entry = cell.getItem();
            if (entry == null) return;
            try { Desktop.getDesktop().open(new File(entry.path()).getParentFile()); }
            catch (Exception ex) { setStatus("Could not open folder: " + ex.getMessage(), true); }
        });
        deleteItem.setOnAction(e -> {
            VideoEntry entry = cell.getItem();
            if (entry == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete \"" + entry.name() + "\" from disk?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    videoManager.removeEntry(entry.id(), true);
                    refreshVideoList();
                    updateStorageIndicator();
                    updateExportButtonState();
                }
            });
        });

        return cell;
    }

    private void refreshVideoList() {
        if (videoList == null) return;
        VideoEntry sel = videoList.getSelectionModel().getSelectedItem();
        videoList.getItems().setAll(videoManager.getEntries());
        if (sel != null) {
            final String id = sel.id();
            videoList.getItems().stream()
                    .filter(e -> e.id().equals(id))
                    .findFirst()
                    .ifPresent(e -> videoList.getSelectionModel().select(e));
        }
    }

    private void updateStorageIndicator() {
        if (storageBar == null || storageLabel == null
                || videoManager == null || gallerySettings == null) return;
        long used = videoManager.getTotalUsedBytes();
        long max  = gallerySettings.getMaxSpaceBytes();
        if (max < 0) {
            storageBar.setProgress(0);
            storageBar.setStyle("");
            storageLabel.setText("No limit  •  " + formatBytes(used) + " used");
        } else {
            double pct = (double) used / max;
            storageBar.setProgress(Math.min(pct, 1.0));
            int pctInt = (int) Math.round(pct * 100);
            storageLabel.setText(formatBytes(used) + " / " + formatBytes(max)
                    + "  (" + pctInt + "% used)");
            if (pct >= 1.0) {
                storageBar.setStyle("-fx-accent: #e05555;");
            } else if (pct >= 0.80) {
                storageBar.setStyle("-fx-accent: #d4a847;");
            } else {
                storageBar.setStyle("-fx-accent: #4caf50;");
            }
        }
    }

    private void updateExportButtonState() {
        if (exportButton == null) return;
        boolean noImage    = rawEvenImage == null;
        boolean atCapacity = videoManager != null
                && gallerySettings != null
                && videoManager.isSpaceAtCapacity(gallerySettings);
        exportButton.setDisable(noImage || atCapacity);
        if (exportEffectsButton != null) {
            exportEffectsButton.setDisable(noImage || atCapacity);
        }
        if (atCapacity && !noImage) {
            exportButton.setTooltip(new Tooltip(
                    "Storage limit reached. Free space or raise the limit in Video Gallery → Settings."));
            if (exportEffectsButton != null) {
                exportEffectsButton.setTooltip(new Tooltip(
                        "Storage limit reached. Free space or raise the limit in Video Gallery → Settings."));
            }
        } else {
            exportButton.setTooltip(null);
            if (exportEffectsButton != null) {
                exportEffectsButton.setTooltip(new Tooltip(
                        "Export transparent PNG frames containing only effect layers."));
            }
        }
    }

    private void showGallerySettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Video Gallery Settings");
        dialog.setHeaderText("Configure storage limits for generated videos");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(COMPACT_INSETS);

        // Output folder
        Label outLabel  = new Label("Output folder:");
        TextField outField = new TextField(settings.getOutputDirectory().toString());
        outField.setPrefWidth(280);
        Button browseBtn = new Button("Browse…");
        browseBtn.setStyle(BUTTON_BASE_STYLE);
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Output Folder");
            File dir = dc.showDialog(dialog.getOwner());
            if (dir != null) outField.setText(dir.getAbsolutePath());
        });
        grid.add(outLabel,   0, 0);
        grid.add(outField,   1, 0);
        grid.add(browseBtn,  2, 0);

        // Max space (GB)
        long curGB = gallerySettings.getMaxSpaceBytes() < 0
                ? -1L : gallerySettings.getMaxSpaceBytes() / (1024L * 1024 * 1024);
        Label spaceLabel  = new Label("Max space (GB):");
        Spinner<Integer> spaceSpinner = new Spinner<>(
                new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(
                        -1, 1000, (int) Math.max(-1, curGB)));
        spaceSpinner.setEditable(true);
        spaceSpinner.setPrefWidth(90);
        Label spaceHint = new Label("-1 = unlimited");
        spaceHint.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px;");
        grid.add(spaceLabel,   0, 1);
        grid.add(spaceSpinner, 1, 1);
        grid.add(spaceHint,    2, 1);

        // Max video count
        Label countLabel  = new Label("Max video count:");
        Spinner<Integer> countSpinner = new Spinner<>(
                new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(
                        -1, 9999, gallerySettings.getMaxVideoCount()));
        countSpinner.setEditable(true);
        countSpinner.setPrefWidth(90);
        Label countHint = new Label("-1 = unlimited");
        countHint.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px;");
        grid.add(countLabel,   0, 2);
        grid.add(countSpinner, 1, 2);
        grid.add(countHint,    2, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            // Apply output folder
            String outPath = outField.getText().trim();
            if (!outPath.isBlank()) {
                settings.setOutputDirectory(Path.of(outPath));
                outputDirField.setText(outPath);
            }
            // Apply space limit
            int spGB = spaceSpinner.getValue();
            gallerySettings.setMaxSpaceBytes(
                    spGB < 0 ? -1L : (long) spGB * 1024 * 1024 * 1024);
            // Apply count limit
            gallerySettings.setMaxVideoCount(countSpinner.getValue());
            // Enforce policy immediately
            videoManager.enforcePolicy(gallerySettings);
            refreshVideoList();
            updateStorageIndicator();
            updateExportButtonState();
        });
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }

    private static String formatDate(long millis) {
        return new SimpleDateFormat("MMM d, yyyy  h:mm a").format(new Date(millis));
    }

    private VBox buildPreviewBox() {
        previewTitle = new Label("Preview — Faded");
        previewTitle.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");

        previewImageView = new ImageView();
        previewImageView.setFitWidth(560);
        previewImageView.setFitHeight(390);
        previewImageView.setPreserveRatio(true);

        StackPane previewPane = new StackPane(previewImageView);
        previewPane.setPrefSize(560, 390);
        previewPane.setStyle("-fx-background-color: #111111; -fx-cursor: hand;");
        previewPane.setBorder(new Border(new BorderStroke(
                Color.web("#444444"), BorderStrokeStyle.SOLID,
                new CornerRadii(4), new BorderWidths(1))));
        Tooltip.install(previewPane, new Tooltip("Drop an image here to replace the current one, or click to browse."));
        configureImageInputTarget(previewPane);

        previewImageView.fitWidthProperty().bind(previewPane.widthProperty().subtract(10));
        previewImageView.fitHeightProperty().bind(previewPane.heightProperty().subtract(10));

        Button prevFrameButton = new Button("<");
        prevFrameButton.setStyle(BUTTON_BASE_STYLE);
        prevFrameButton.setOnAction(e -> {
            stopPreviewPlayback();
            setPreviewFrame(previewFrameIndex - 1);
        });

        Button nextFrameButton = new Button("> ");
        nextFrameButton.setStyle(BUTTON_BASE_STYLE);
        nextFrameButton.setOnAction(e -> {
            stopPreviewPlayback();
            setPreviewFrame(previewFrameIndex + 1);
        });

        previewPlayButton = new Button("Play");
        previewPlayButton.setStyle(BUTTON_BASE_STYLE);
        previewPlayButton.setOnAction(e -> {
            if (isPreviewPlaying()) {
                stopPreviewPlayback();
            } else {
                startPreviewPlayback();
            }
        });

        previewLoopCheck = new CheckBox("Loop");
        previewLoopCheck.setSelected(true);
        previewLoopCheck.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");

        effectsOnlyPreviewCheck = new CheckBox("Effects-only Alpha");
        effectsOnlyPreviewCheck.setSelected(false);
        effectsOnlyPreviewCheck.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");
        effectsOnlyPreviewCheck.selectedProperty().addListener((obs, old, selected) -> renderPreviewFrame());

        previewSecondsSpinner = new Spinner<>(PREVIEW_MIN_SECONDS, PREVIEW_MAX_SECONDS, 2);
        previewSecondsSpinner.setPrefWidth(70);
        previewSecondsSpinner.setEditable(true);
        previewSecondsSpinner.valueProperty().addListener((obs, old, val) -> {
            stopPreviewPlayback();
            updatePreviewFrameBounds();
            renderPreviewFrame();
        });

        previewFpsCombo = new ComboBox<>();
        previewFpsCombo.getItems().addAll(30, 60);
        previewFpsCombo.setValue(30);
        previewFpsCombo.setPrefWidth(70);
        previewFpsCombo.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-size: 11px;");
        previewFpsCombo.valueProperty().addListener((obs, old, val) -> {
            stopPreviewPlayback();
            updatePreviewFrameBounds();
            renderPreviewFrame();
        });

        previewFrameSlider = new Slider(0, 59, 0);
        previewFrameSlider.setPrefWidth(250);
        previewFrameSlider.setOnMousePressed(e -> stopPreviewPlayback());
        previewFrameSlider.valueProperty().addListener((obs, old, val) -> {
            if (previewFrameSlider.isValueChanging()) {
                previewFrameIndex = (int) Math.round(val.doubleValue());
                renderPreviewFrame();
            }
        });
        previewFrameSlider.setOnMouseReleased(e -> {
            previewFrameIndex = (int) Math.round(previewFrameSlider.getValue());
            renderPreviewFrame();
        });

        previewFrameLabel = new Label("Frame 1 / 60");
        previewFrameLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

        Label durationLabel = new Label("Sec:");
        durationLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");
        Label fpsLabel = new Label("FPS:");
        fpsLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");

        HBox controlsRow = new HBox(5,
                prevFrameButton, nextFrameButton, previewPlayButton,
            previewLoopCheck, effectsOnlyPreviewCheck,
                durationLabel, previewSecondsSpinner,
                fpsLabel, previewFpsCombo,
                previewFrameSlider, previewFrameLabel);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, previewTitle, previewPane, controlsRow);
        box.setPadding(COMPACT_INSETS);
        box.setAlignment(Pos.TOP_LEFT);

        updatePreviewFrameBounds();
        updatePreviewFrameLabel();
        return box;
    }

    private void configureImageInputTarget(StackPane target) {
        target.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        target.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                loadImageFile(db.getFiles().get(0));
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        target.setOnMouseClicked(e -> {
            File file = chooseImageFile(target);
            if (file != null) {
                loadImageFile(file);
            }
        });
    }

    private File chooseImageFile(StackPane target) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        return chooser.showOpenDialog(target.getScene().getWindow());
    }

    // ── Presets + effects (middle panel) ────────────────────────────────────
    private VBox buildPresetAndEffectsPanel(Stage stage) {
        // Preset selector
        Label presetLabel = new Label("Effect:");
        presetLabel.setStyle("-fx-text-fill: #cccccc;");
        presetCombo = new ComboBox<>();
        presetCombo.getItems().addAll(Preset.values());
        presetCombo.setPrefWidth(170);
        presetCombo.setPrefHeight(24);
        presetCombo.setValue(Preset.FIFTIES_FILM);
        presetCombo.setStyle(
                "-fx-background-color: #ffffff; "
                + "-fx-text-fill: #000000; "
                + "-fx-control-inner-background: #ffffff; "
                + "-fx-focus-color: #d4a847; "
            + "-fx-faint-focus-color: #d4a84722; "
            + "-fx-padding: 2;");
        presetCombo.valueProperty().addListener((obs, old, val) -> {
            applyPresetDefaults(val);
            refreshPreviewIfLoaded();
        });

        Label customPresetLabel = new Label("Custom:");
        customPresetLabel.setStyle("-fx-text-fill: #cccccc;");
        customPresetCombo = new ComboBox<>();
        customPresetCombo.setPrefWidth(190);
        customPresetCombo.setPrefHeight(24);
        customPresetCombo.setStyle(
                "-fx-background-color: #ffffff; "
                        + "-fx-text-fill: #000000; "
                        + "-fx-control-inner-background: #ffffff; "
                + "-fx-padding: 2; -fx-font-size: 11px;");
        refreshCustomPresetCombo();
        customPresetCombo.setOnAction(e -> {
            String selected = customPresetCombo.getValue();
            if (selected == null || selected.isBlank()) {
                return;
            }
            PresetManager.PresetData data = presetManager.getCustomPreset(selected);
            if (data == null) {
                return;
            }
            applyCustomPreset(data);
            refreshPreviewIfLoaded();
            setStatus("Applied custom preset: " + selected, false);
        });

        Button addPresetButton = new Button("Add");
        addPresetButton.setStyle(BUTTON_BASE_STYLE);
        addPresetButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Custom Preset");
            dialog.setHeaderText(null);
            dialog.setContentText("Preset name:");
            Optional<String> input = dialog.showAndWait();
            if (input.isEmpty()) {
                return;
            }
            String name = input.get().trim();
            if (name.isBlank()) {
                setStatus("Preset name cannot be blank.", true);
                return;
            }
            if (PresetManager.isBuiltInPreset(name)) {
                setStatus("That name is reserved for built-in presets.", true);
                return;
            }
            presetManager.savePreset(name, captureCurrentPresetData(name));
            refreshCustomPresetCombo();
            customPresetCombo.setValue(name);
            setStatus("Saved custom preset: " + name, false);
        });

        Button updatePresetButton = new Button("Update");
        updatePresetButton.setStyle(BUTTON_BASE_STYLE);
        updatePresetButton.setOnAction(e -> {
            String selected = customPresetCombo.getValue();
            if (selected == null || selected.isBlank()) {
                setStatus("Select a custom preset to update.", true);
                return;
            }
            presetManager.savePreset(selected, captureCurrentPresetData(selected));
            setStatus("Updated custom preset: " + selected, false);
        });

        Button deletePresetButton = new Button("Delete");
        deletePresetButton.setStyle(BUTTON_BASE_STYLE);
        deletePresetButton.setOnAction(e -> {
            String selected = customPresetCombo.getValue();
            if (selected == null || selected.isBlank()) {
                setStatus("Select a custom preset to delete.", true);
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete custom preset \"" + selected + "\"?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.setTitle("Delete Preset");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    presetManager.deletePreset(selected);
                    refreshCustomPresetCombo();
                    setStatus("Deleted custom preset: " + selected, false);
                }
            });
        });

        // Duration row
        Label durationLabel = new Label("Duration (seconds):");
        durationLabel.setStyle("-fx-text-fill: #cccccc;");
        durationSpinner = new Spinner<>(1, 300, settings.getDurationSeconds());
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(130);
        durationSpinner.setPrefHeight(24);
        durationSpinner.setStyle("-fx-padding: 2;");
        durationSpinner.getEditor().setStyle("-fx-padding: 2;");
        durationSpinner.valueProperty().addListener((obs, old, val) -> settings.setDurationSeconds(val));

        // Output folder row
        Label outputLabel = new Label("Output folder:");
        outputLabel.setStyle("-fx-text-fill: #cccccc;");
        outputDirField = new TextField(settings.getOutputDirectory().toString());
        outputDirField.setPrefWidth(470);
        outputDirField.setPrefHeight(24);
        outputDirField.setStyle(
            "-fx-background-color: #2a2a2a; -fx-text-fill: #cccccc; -fx-border-color: #555555; -fx-padding: 2; -fx-font-size: 11px;");
        outputDirField.textProperty().addListener((obs, old, val) -> {
            if (!val.isBlank()) {
                settings.setOutputDirectory(Path.of(val.trim()));
            }
        });

        Button browseButton = new Button("Browse…");
        browseButton.setStyle(BUTTON_BASE_STYLE);
        browseButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Output Folder");
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                outputDirField.setText(dir.getAbsolutePath());
                settings.setOutputDirectory(dir.toPath());
            }
        });

        HBox settingsRow = new HBox(8,
                presetLabel, presetCombo,
            customPresetLabel, customPresetCombo,
            addPresetButton, updatePresetButton, deletePresetButton,
                durationLabel, durationSpinner,
                outputLabel, outputDirField, browseButton);
        settingsRow.setAlignment(Pos.CENTER_LEFT);

        GridPane effectsGrid = buildEffectsGrid();
        ScrollPane effectsScrollPane = new ScrollPane(effectsGrid);
        effectsScrollPane.setFitToWidth(true);
        effectsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        effectsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        effectsScrollPane.setPannable(true);
        effectsScrollPane.setPrefViewportHeight(250);
        effectsScrollPane.setMinViewportHeight(220);
        effectsScrollPane.setStyle("-fx-background: #1e1e1e; -fx-background-color: #1e1e1e;");

        // Randomize / Reset buttons placed just above the effects grid
        Button randomizeButton = new Button("\uD83C\uDFB2  Randomize");
        randomizeButton.setStyle(
                "-fx-background-color: #4a6fa5; -fx-text-fill: #ffffff; "
                + "-fx-font-weight: bold; -fx-padding: 5 5 5 5;");
        randomizeButton.setOnAction(e -> randomizeEffects());

        Button resetButton = new Button("Reset All");
        resetButton.setStyle(
                "-fx-background-color: #555555; -fx-text-fill: #cccccc; "
                + "-fx-padding: 5 5 5 5;");
        resetButton.setOnAction(e -> {
            presetCombo.setValue(Preset.NONE);
            resetAllEffects();
            refreshPreviewIfLoaded();
        });

        HBox effectButtonRow = new HBox(5, randomizeButton, resetButton);
        effectButtonRow.setAlignment(Pos.CENTER_LEFT);
        effectButtonRow.setPadding(new Insets(0, 0, 5, 0));

        VBox middlePanel = new VBox(5, new Separator(), settingsRow, effectButtonRow, effectsScrollPane);
        middlePanel.setStyle("-fx-padding: 5 0 0 0;");
        VBox.setVgrow(effectsScrollPane, Priority.ALWAYS);

        applyPresetDefaults(presetCombo.getValue());
        return middlePanel;
    }

    // ── Fixed bottom bars (export + status) ─────────────────────────────────
    private VBox buildBottomBars() {
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(420);
        progressBar.setVisible(false);

        exportButton = new Button("Export Video");
        exportButton.setStyle(
                "-fx-background-color: #d4a847; -fx-text-fill: #1e1e1e; "
                        + "-fx-font-weight: bold; -fx-padding: 2;");
        exportButton.setDisable(true);
        exportButton.setOnAction(e -> exportVideo());

        exportEffectsButton = new Button("Export Effects Alpha");
        exportEffectsButton.setStyle(BUTTON_BASE_STYLE);
        exportEffectsButton.setDisable(true);
        exportEffectsButton.setOnAction(e -> exportEffectsAlphaSequence());

        HBox exportRow = new HBox(8, exportButton, exportEffectsButton, progressBar);
        exportRow.setAlignment(Pos.CENTER_LEFT);
        exportRow.setPadding(new Insets(5, 0, 5, 0));

        statusLabel = new Label("Ready");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(2, 6, 2, 6));
        statusBar.setStyle("-fx-background-color: #242424; -fx-border-color: #3a3a3a; -fx-border-width: 1 0 0 0;");

        return new VBox(exportRow, statusBar);
    }

    private void refreshCustomPresetCombo() {
        if (customPresetCombo == null) {
            return;
        }
        String previous = customPresetCombo.getValue();
        customPresetCombo.getItems().setAll(presetManager.getCustomPresetNames());
        if (previous != null && customPresetCombo.getItems().contains(previous)) {
            customPresetCombo.setValue(previous);
        } else if (!customPresetCombo.getItems().isEmpty()) {
            customPresetCombo.setValue(customPresetCombo.getItems().get(0));
        } else {
            customPresetCombo.setValue(null);
        }
    }

    private PresetManager.PresetData captureCurrentPresetData(String name) {
        return new PresetManager.PresetData(
                name,
                lightLeakControls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(lightLeakControls.variance.getValue()),
                (int) Math.round(lightLeakControls.frequency.getValue()),
                (int) Math.round(lightLeakControls.randomness.getValue()),
                vignetteControls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(vignetteControls.variance.getValue()),
                (int) Math.round(vignetteControls.frequency.getValue()),
                (int) Math.round(vignetteControls.randomness.getValue()),
                grainControls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(grainControls.variance.getValue()),
                (int) Math.round(grainControls.frequency.getValue()),
                (int) Math.round(grainControls.randomness.getValue()),
                dustControls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(dustControls.variance.getValue()),
                (int) Math.round(dustControls.frequency.getValue()),
                (int) Math.round(dustControls.randomness.getValue()),
                scratchesControls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(scratchesControls.variance.getValue()),
                (int) Math.round(scratchesControls.frequency.getValue()),
                (int) Math.round(scratchesControls.randomness.getValue()),
                trackingHControls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(trackingHControls.variance.getValue()),
                (int) Math.round(trackingHControls.frequency.getValue()),
                (int) Math.round(trackingHControls.randomness.getValue()),
                flickerControls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(flickerControls.variance.getValue()),
                (int) Math.round(flickerControls.frequency.getValue()),
                (int) Math.round(flickerControls.randomness.getValue()));
    }

    private void applyCustomPreset(PresetManager.PresetData data) {
        setEffect(lightLeakControls, data.lightLeakMode(), data.lightLeakVar(), data.lightLeakFreq(), data.lightLeakRand());
        setEffect(vignetteControls, data.vignetteMode(), data.vignetteVar(), data.vignetteFreq(), data.vignetteRand());
        setEffect(grainControls, data.grainMode(), data.grainVar(), data.grainFreq(), data.grainRand());
        setEffect(dustControls, data.dustMode(), data.dustVar(), data.dustFreq(), data.dustRand());
        setEffect(scratchesControls, data.scratchesMode(), data.scratchesVar(), data.scratchesFreq(), data.scratchesRand());
        setEffect(trackingHControls, data.trackingMode(), data.trackingVar(), data.trackingFreq(), data.trackingRand());
        setEffect(trackingVControls, data.trackingMode(), data.trackingVar(), data.trackingFreq(), data.trackingRand());
        setEffect(flickerControls, data.flickerMode(), data.flickerVar(), data.flickerFreq(), data.flickerRand());
    }

    private GridPane buildEffectsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(COMPACT_INSETS);

        addEffectsHeader(grid);

        lightLeakControls  = createEffectControls(EffectMode.NONE, 30, 45, 0);
        vignetteControls   = createEffectControls(EffectMode.NONE, 25,  0, 0);
        grainControls      = createEffectControls(EffectMode.NONE, 30, 45, 0);
        dustControls       = createEffectControls(EffectMode.NONE, 25, 35, 0);
        scratchesControls  = createEffectControls(EffectMode.NONE, 25, 35,  0);
        trackingHControls  = createEffectControls(EffectMode.NONE, 20, 70, 40);
        trackingVControls  = createEffectControls(EffectMode.NONE, 15, 30, 25);
        flickerControls    = createEffectControls(EffectMode.NONE, 18, 55,  0);

        addEffectRow(grid, 1, "Light Leak",      lightLeakControls);
        addEffectRow(grid, 2, "Vignette",         vignetteControls);
        addEffectRow(grid, 3, "Grain",             grainControls);
        addEffectRow(grid, 4, "Dust / Lint",       dustControls);
        addEffectRow(grid, 5, "Film Scratches",   scratchesControls);
        addEffectRow(grid, 6, "Track. Horiz.",    trackingHControls);
        addEffectRow(grid, 7, "Track. Vert.",     trackingVControls);
        addEffectRow(grid, 8, "Flicker",           flickerControls);

        return grid;
    }

    private void addEffectsHeader(GridPane grid) {
        Label c1 = new Label("Effect");
        Label c2 = new Label("On/Off");
        Label c3 = new Label("Variance");
        Label c4 = new Label("Frequency");
        Label c5 = new Label("Randomness");
        Label c6 = new Label("Pattern");
        for (Label label : new Label[]{c1, c2, c3, c4, c5, c6}) {
            label.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
        grid.add(c1, 0, 0);
        grid.add(c2, 1, 0);
        grid.add(c3, 2, 0);
        grid.add(c4, 3, 0);
        grid.add(c5, 4, 0);
        grid.add(c6, 5, 0);
    }

    private EffectControls createEffectControls(EffectMode modeDefault, int varianceDefault, int frequencyDefault, int randomnessDefault) {
        ToggleButton modeToggle = new ToggleButton();
        modeToggle.setPrefWidth(78);
        modeToggle.setPrefHeight(20);
        modeToggle.setMinHeight(20);

        Slider variance = createSlider(varianceDefault);
        Label varianceValue = createValueLabel(varianceDefault);

        Slider frequency = createSlider(frequencyDefault);
        Label frequencyValue = createValueLabel(frequencyDefault);

        Slider randomness = createSlider(randomnessDefault);
        Label randomnessValue = createValueLabel(randomnessDefault);

        Canvas waveform = new Canvas(130, 26);

        EffectControls controls = new EffectControls(
            modeToggle, variance, varianceValue, frequency, frequencyValue, randomness, randomnessValue, waveform);

        modeToggle.selectedProperty().addListener((obs, old, isOn) -> {
            applyModeToggleVisual(modeToggle, isOn);
            setEffectControlsEnabled(controls, isOn);
            refreshPreviewIfLoaded();
        });

        // Single redraw runnable captures all three sliders by reference
        Runnable redraw = () -> redrawWaveform(
                waveform,
                variance.getValue(),
                frequency.getValue(),
                randomness.getValue());

        variance.valueProperty().addListener((obs, old, val) -> {
            varianceValue.setText(Integer.toString(val.intValue()));
            refreshPreviewIfLoaded();
            redraw.run();
        });
        frequency.valueProperty().addListener((obs, old, val) -> {
            frequencyValue.setText(Integer.toString(val.intValue()));
            refreshPreviewIfLoaded();
            redraw.run();
        });
        randomness.valueProperty().addListener((obs, old, val) -> {
            randomnessValue.setText(Integer.toString(val.intValue()));
            refreshPreviewIfLoaded();
            redraw.run();
        });

        boolean defaultOn = modeDefault == EffectMode.ON;
        modeToggle.setSelected(defaultOn);
        applyModeToggleVisual(modeToggle, defaultOn);
        setEffectControlsEnabled(controls, defaultOn);
        redraw.run();

        return controls;
    }

    private void applyModeToggleVisual(ToggleButton toggle, boolean on) {
        if (on) {
            toggle.setText("ON");
            toggle.setStyle(TOGGLE_ON_STYLE);
        } else {
            toggle.setText("OFF");
            toggle.setStyle(TOGGLE_OFF_STYLE);
        }
    }

    private void setEffectControlsEnabled(EffectControls controls, boolean enabled) {
        controls.variance.setDisable(!enabled);
        controls.frequency.setDisable(!enabled);
        controls.randomness.setDisable(!enabled);
        controls.varianceValue.setDisable(!enabled);
        controls.frequencyValue.setDisable(!enabled);
        controls.randomnessValue.setDisable(!enabled);
        controls.waveform.setOpacity(enabled ? 1.0 : 0.35);
    }

    private Slider createSlider(int value) {
        Slider slider = new Slider(0, 100, value);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(4);
        slider.setSnapToTicks(false);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setPrefWidth(130);
        slider.setPrefHeight(16);
        slider.setStyle("-fx-padding: 0 2 0 2;");
        return slider;
    }

    private Label createValueLabel(int value) {
        Label label = new Label(Integer.toString(value));
        label.setStyle("-fx-text-fill: #bbbbbb; -fx-font-size: 10px;");
        label.setMinWidth(28);
        return label;
    }

    private void addEffectRow(GridPane grid, int row, String name, EffectControls controls) {
        Label effectLabel = new Label(name);
        effectLabel.setStyle("-fx-text-fill: #cccccc;");
        effectLabel.setMinWidth(90);

        HBox varianceBox = new HBox(5, controls.variance, controls.varianceValue);
        varianceBox.setAlignment(Pos.CENTER_LEFT);

        HBox frequencyBox = new HBox(5, controls.frequency, controls.frequencyValue);
        frequencyBox.setAlignment(Pos.CENTER_LEFT);

        HBox randomnessBox = new HBox(5, controls.randomness, controls.randomnessValue);
        randomnessBox.setAlignment(Pos.CENTER_LEFT);

        StackPane waveformPane = new StackPane(controls.waveform);
        waveformPane.setStyle("-fx-background-color: #111111; -fx-border-color: #333333; -fx-border-width: 1;");

        grid.add(effectLabel,   0, row);
        grid.add(controls.modeToggle, 1, row);
        grid.add(varianceBox,   2, row);
        grid.add(frequencyBox,  3, row);
        grid.add(randomnessBox, 4, row);
        grid.add(waveformPane,  5, row);
    }

    private void applyPresetDefaults(Preset preset) {
        if (preset == null) {
            return;
        }

        applyingPresetDefaults = true;
        try {
            resetAllEffects();
            switch (preset) {
                // 20s Reel: B&W, strong vignette, heavy grain, scratches, irregular flicker
                case TWENTIES_REEL -> {
                    setEffect(lightLeakControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(vignetteControls,   EffectMode.ON,   70,  0, 15);
                    setEffect(grainControls,      EffectMode.ON,   65, 50, 45);
                    setEffect(dustControls,       EffectMode.ON,   45, 65, 55);
                    setEffect(scratchesControls,  EffectMode.ON,   50, 55, 60);
                    setEffect(trackingHControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(trackingVControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(flickerControls,    EffectMode.ON,   40, 45, 65);
                }
                // 50s Film: faded warm color, mild grain + vignette, gentle flicker
                case FIFTIES_FILM -> {
                    setEffect(lightLeakControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(vignetteControls,   EffectMode.ON,   28,  0, 10);
                    setEffect(grainControls,      EffectMode.ON,   35, 35, 30);
                    setEffect(dustControls,       EffectMode.ON,   20, 35, 20);
                    setEffect(scratchesControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(trackingHControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(trackingVControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(flickerControls,    EffectMode.ON,   14, 30, 25);
                }
                // 80s VHS: faded color, scan lines (built-in), jittery tracking, fast grain
                case EIGHTIES_VHS -> {
                    setEffect(lightLeakControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(vignetteControls,   EffectMode.NONE,  0,  0,  0);
                    setEffect(grainControls,      EffectMode.ON,   26, 70, 55);
                    setEffect(dustControls,       EffectMode.NONE,  0,  0,  0);
                    setEffect(scratchesControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(trackingHControls,  EffectMode.ON,   35, 75, 70);
                    setEffect(trackingVControls,  EffectMode.ON,   25, 60, 50);
                    setEffect(flickerControls,    EffectMode.ON,   22, 65, 50);
                }
                // Faded: warm yellow color, light grain + vignette
                case FADED -> {
                    setEffect(lightLeakControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(vignetteControls,   EffectMode.ON,   12,  0,  0);
                    setEffect(grainControls,      EffectMode.ON,   28, 32, 20);
                    setEffect(dustControls,       EffectMode.NONE,  0,  0,  0);
                    setEffect(scratchesControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(trackingHControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(trackingVControls,  EffectMode.NONE,  0,  0,  0);
                    setEffect(flickerControls,    EffectMode.NONE,  0,  0,  0);
                }
                case SEPIA, NONE -> {
                    // All effects off — pure sepia / no-effect.
                }
            }
        } finally {
            applyingPresetDefaults = false;
        }
    }

    private void randomizeEffects() {
        Random rng = new Random();
        // Helper: randomly decide if an effect is ON; `onChance` is 0.0-1.0
        // Each effect has its own on-chance and slider ranges so results stay
        // film-plausible rather than totally garish.
        applyingPresetDefaults = true;
        try {
            randomizeOne(rng, lightLeakControls,  0.40f,  10, 70,   0, 80,  0, 60);
            randomizeOne(rng, vignetteControls,   0.65f,  10, 80,   0,  5,  0, 40);
            randomizeOne(rng, grainControls,      0.75f,  10, 80,  20, 90,  0, 70);
            randomizeOne(rng, dustControls,       0.55f,  10, 70,  20, 80,  0, 70);
            randomizeOne(rng, scratchesControls,  0.45f,  10, 65,  20, 80,  0, 70);
            randomizeOne(rng, trackingHControls,  0.50f,  10, 70,  20, 90, 20, 80);
            randomizeOne(rng, trackingVControls,  0.40f,  10, 55,  10, 70, 10, 60);
            randomizeOne(rng, flickerControls,    0.60f,  10, 70,  10, 85,  0, 70);
        } finally {
            applyingPresetDefaults = false;
        }
        refreshPreviewIfLoaded();
    }

    /**
     * Randomly enables or disables one effect row, and if enabled picks
     * slider values within the supplied [min, max] ranges.
     */
    private void randomizeOne(Random rng, EffectControls controls,
            float onChance,
            int varMin, int varMax,
            int freqMin, int freqMax,
            int rndMin,  int rndMax) {
        if (rng.nextFloat() < onChance) {
            int v = varMin  + rng.nextInt(varMax  - varMin  + 1);
            int f = freqMin + rng.nextInt(freqMax - freqMin + 1);
            int r = rndMin  + rng.nextInt(rndMax  - rndMin  + 1);
            setEffect(controls, EffectMode.ON, v, f, r);
        } else {
            setEffect(controls, EffectMode.NONE, 0, 0, 0);
        }
    }

    private void resetAllEffects() {
        setEffect(lightLeakControls,  EffectMode.NONE, 0, 0, 0);
        setEffect(vignetteControls,   EffectMode.NONE, 0, 0, 0);
        setEffect(grainControls,      EffectMode.NONE, 0, 0, 0);
        setEffect(dustControls,       EffectMode.NONE, 0, 0, 0);
        setEffect(scratchesControls,  EffectMode.NONE, 0, 0, 0);
        setEffect(trackingHControls,  EffectMode.NONE, 0, 0, 0);
        setEffect(trackingVControls,  EffectMode.NONE, 0, 0, 0);
        setEffect(flickerControls,    EffectMode.NONE, 0, 0, 0);
    }

    private void setEffect(EffectControls controls, EffectMode mode, int variance, int frequency, int randomness) {
        controls.modeToggle.setSelected(mode == EffectMode.ON);
        controls.variance.setValue(variance);
        controls.frequency.setValue(frequency);
        controls.randomness.setValue(randomness);
        controls.varianceValue.setText(Integer.toString(variance));
        controls.frequencyValue.setText(Integer.toString(frequency));
        controls.randomnessValue.setText(Integer.toString(randomness));
    }

    private int getPreviewFps() {
        Integer fps = previewFpsCombo != null ? previewFpsCombo.getValue() : null;
        return fps == null ? 30 : fps;
    }

    private int getPreviewTotalFrames() {
        int seconds = previewSecondsSpinner != null ? previewSecondsSpinner.getValue() : 2;
        return Math.max(1, seconds * getPreviewFps());
    }

    private void updatePreviewFrameBounds() {
        if (previewFrameSlider == null) {
            return;
        }
        int totalFrames = getPreviewTotalFrames();
        previewFrameIndex = Math.max(0, Math.min(previewFrameIndex, totalFrames - 1));
        previewFrameSlider.setMin(0);
        previewFrameSlider.setMax(totalFrames - 1);
        previewFrameSlider.setValue(previewFrameIndex);
        updatePreviewFrameLabel();
    }

    private void updatePreviewFrameLabel() {
        if (previewFrameLabel == null) {
            return;
        }
        int totalFrames = getPreviewTotalFrames();
        previewFrameLabel.setText("Frame " + (previewFrameIndex + 1) + " / " + totalFrames);
    }

    private void setPreviewFrame(int frameIndex) {
        int totalFrames = getPreviewTotalFrames();
        previewFrameIndex = Math.max(0, Math.min(frameIndex, totalFrames - 1));
        if (previewFrameSlider != null) {
            previewFrameSlider.setValue(previewFrameIndex);
        }
        renderPreviewFrame();
    }

    private boolean isPreviewPlaying() {
        return previewTimeline != null && previewTimeline.getStatus() == Timeline.Status.RUNNING;
    }

    private void startPreviewPlayback() {
        if (rawEvenImage == null) {
            return;
        }
        stopPreviewPlayback();
        int fps = getPreviewFps();
        previewTimeline = new Timeline(new KeyFrame(Duration.millis(1000.0 / fps), e -> {
            int total = getPreviewTotalFrames();
            if (previewFrameIndex >= total - 1) {
                if (previewLoopCheck != null && previewLoopCheck.isSelected()) {
                    previewFrameIndex = 0;
                } else {
                    stopPreviewPlayback();
                    return;
                }
            } else {
                previewFrameIndex++;
            }
            if (previewFrameSlider != null) {
                previewFrameSlider.setValue(previewFrameIndex);
            }
            renderPreviewFrame();
        }));
        previewTimeline.setCycleCount(Timeline.INDEFINITE);
        previewTimeline.play();
        if (previewPlayButton != null) {
            previewPlayButton.setText("Pause");
        }
    }

    private void stopPreviewPlayback() {
        if (previewTimeline != null) {
            previewTimeline.stop();
        }
        if (previewPlayButton != null) {
            previewPlayButton.setText("Play");
        }
    }

    private void renderPreviewFrame() {
        if (rawEvenImage == null) {
            return;
        }
        int totalFrames = getPreviewTotalFrames();
        previewFrameIndex = Math.max(0, Math.min(previewFrameIndex, totalFrames - 1));
        EffectConfig config = buildEffectConfig();
        boolean effectsOnly = effectsOnlyPreviewCheck != null && effectsOnlyPreviewCheck.isSelected();
        processedImage = imageProcessor.applyEffects(rawEvenImage, config, previewFrameIndex, totalFrames);
        BufferedImage toDisplay = effectsOnly
                ? imageProcessor.applyEffectsOnlyAlphaLayer(rawEvenImage, config, previewFrameIndex, totalFrames)
                : processedImage;
        previewTitle.setText("Preview — " + presetCombo.getValue());
        javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(toDisplay, null);
        previewImageView.setImage(fxImage);
        updatePreviewFrameLabel();
    }

    private void refreshPreviewIfLoaded() {
        if (applyingPresetDefaults) {
            return;
        }
        if (rawEvenImage != null) {
            renderPreviewFrame();
        }
    }

    private EffectConfig.EffectSetting effectSettingOf(EffectControls controls) {
        return new EffectConfig.EffectSetting(
                controls.modeToggle.isSelected() ? EffectMode.ON : EffectMode.NONE,
                (int) Math.round(controls.variance.getValue()),
                (int) Math.round(controls.frequency.getValue()),
                (int) Math.round(controls.randomness.getValue()));
    }

    private EffectConfig buildEffectConfig() {
        return new EffectConfig(
                presetCombo.getValue(),
                effectSettingOf(lightLeakControls),
                effectSettingOf(vignetteControls),
                effectSettingOf(grainControls),
                effectSettingOf(dustControls),
                effectSettingOf(scratchesControls),
                effectSettingOf(trackingHControls),
                effectSettingOf(trackingVControls),
                effectSettingOf(flickerControls));
    }

    // ── Logic ────────────────────────────────────────────────────────────────
    private void loadImageFile(File file) {
        try {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) {
                setStatus("Could not read file. Make sure it is a valid image.", true);
                return;
            }
            stopPreviewPlayback();
            rawEvenImage = imageProcessor.ensureEvenDimensions(raw);
            currentImageFile = file;
            previewFrameIndex = 0;
            updatePreviewFrameBounds();
            refreshPreviewIfLoaded();
            // Select matching gallery entry if present
            String absPath = file.getAbsolutePath();
            galleryList.getItems().stream()
                    .filter(e -> new File(e.path()).getAbsolutePath().equals(absPath))
                    .findFirst()
                    .ifPresent(e -> galleryList.getSelectionModel().select(e));
            updateExportButtonState();
            setStatus("Image loaded. Ready to export.", false);
        } catch (Exception ex) {
            setStatus("Error loading image: " + ex.getMessage(), true);
        }
    }

    private void exportVideo() {
        if (processedImage == null) return;

        // Guard: check storage capacity before starting
        if (videoManager != null && gallerySettings != null
                && videoManager.isSpaceAtCapacity(gallerySettings)) {
            setStatus("Storage limit reached. Free space or raise the limit in Video Gallery → Settings.", true);
            return;
        }

        exportButton.setDisable(true);
        if (exportEffectsButton != null) {
            exportEffectsButton.setDisable(true);
        }
        progressBar.setStyle("");
        progressBar.setOnMouseClicked(null);
        progressBar.setCursor(javafx.scene.Cursor.DEFAULT);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        setStatus("Generating video…", false);

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                EffectConfig config = buildEffectConfig();
                String imageName = currentImageFile != null
                        ? stripExtension(currentImageFile.getName()) : "image";
                String presetName = presetCombo.getValue() != null
                        ? presetCombo.getValue().name().toLowerCase().replace('_', '-') : "none";
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String filename = imageName + "_" + presetName + "_" + ts + ".mp4";
                return videoCreator.createVideo(settings, filename,
                        (frameIndex, totalFrames) -> imageProcessor.applyEffects(rawEvenImage, config, frameIndex, totalFrames));
            }
        };

        task.setOnSucceeded(e -> {
            lastExportedPath = task.getValue();
            progressBar.setProgress(1.0);
            progressBar.setStyle("-fx-accent: #4caf50;");
            progressBar.setCursor(javafx.scene.Cursor.HAND);
            progressBar.setOnMouseClicked(ev -> openVideoInDefaultPlayer(lastExportedPath));
            progressBar.setTooltip(new Tooltip("Click to open: " + lastExportedPath));
            setStatus("Saved: " + lastExportedPath + "  (click progress bar to play)", false);
            // Register in video gallery and enforce retention policy
            if (videoManager != null && gallerySettings != null) {
                String vName = stripExtension(lastExportedPath.getFileName().toString());
                String srcImg = currentImageFile != null ? currentImageFile.getAbsolutePath() : "";
                videoManager.registerVideo(lastExportedPath, vName, srcImg);
                videoManager.enforcePolicy(gallerySettings);
                refreshVideoList();
                updateStorageIndicator();
            }
            updateExportButtonState();
        });

        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            Throwable cause = task.getException();
            setStatus("Export failed: " + (cause != null ? cause.getMessage() : "unknown error"), true);
            updateExportButtonState();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void exportEffectsAlphaSequence() {
        if (rawEvenImage == null) {
            return;
        }
        if (videoManager != null && gallerySettings != null
                && videoManager.isSpaceAtCapacity(gallerySettings)) {
            setStatus("Storage limit reached. Free space or raise the limit in Video Gallery → Settings.", true);
            return;
        }

        exportButton.setDisable(true);
        if (exportEffectsButton != null) {
            exportEffectsButton.setDisable(true);
        }
        progressBar.setStyle("");
        progressBar.setOnMouseClicked(null);
        progressBar.setCursor(javafx.scene.Cursor.DEFAULT);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        setStatus("Exporting transparent effects PNG sequence…", false);

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                EffectConfig config = buildEffectConfig();
                String imageName = currentImageFile != null
                        ? stripExtension(currentImageFile.getName()) : "image";
                String presetName = presetCombo.getValue() != null
                        ? presetCombo.getValue().name().toLowerCase().replace('_', '-') : "none";
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String dirName = imageName + "_" + presetName + "_effects_alpha_" + ts;
                return videoCreator.createTransparentPngSequence(settings, dirName,
                        (frameIndex, totalFrames) -> imageProcessor.applyEffectsOnlyAlphaLayer(
                                rawEvenImage, config, frameIndex, totalFrames));
            }
        };

        task.setOnSucceeded(e -> {
            Path outDir = task.getValue();
            progressBar.setProgress(1.0);
            progressBar.setStyle("-fx-accent: #4caf50;");
            setStatus("Saved transparent effect frames to: " + outDir + " (use in editor as alpha overlay)", false);
            updateExportButtonState();
        });

        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            Throwable cause = task.getException();
            setStatus("Effects export failed: " + (cause != null ? cause.getMessage() : "unknown error"), true);
            updateExportButtonState();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void openVideoInDefaultPlayer(Path path) {
        if (path == null) return;
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (Exception ex) {
            setStatus("Could not open video: " + ex.getMessage(), true);
        }
    }

    private void setStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle(
                    "-fx-text-fill: " + (isError ? "#e05555" : "#888888") + "; -fx-font-size: 12px;");
        });
    }

    /**
     * Draws a signal waveform into the canvas that visualises how the effect will
     * feel over time.  Low randomness = smooth sine; high randomness = noisy.
     */
    private void redrawWaveform(Canvas canvas, double variance, double frequency, double randomness) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#111111"));
        gc.fillRect(0, 0, w, h);

        // Baseline
        gc.setStroke(Color.web("#2a2a2a"));
        gc.setLineWidth(0.5);
        gc.strokeLine(0, h / 2, w, h / 2);

        if (variance < 1) {
            return;
        }

        double hz = 0.5 + frequency / 100.0 * 8.0;
        double normVar = variance / 100.0;
        double normRnd = randomness / 100.0;
        double midY = h / 2.0;
        double halfH = midY - 2;

        Random rng = new Random(42);
        gc.setStroke(Color.web("#d4a847"));
        gc.setLineWidth(1.0);
        gc.beginPath();
        int samples = (int) w;
        for (int i = 0; i < samples; i++) {
            double t = i / (double) (samples - 1);
            double det = Math.sin(t * Math.PI * 2 * hz) * normVar;
            double rndPart = (rng.nextDouble() * 2 - 1) * normVar;
            double y = det * (1 - normRnd) + rndPart * normRnd;
            double py = midY - y * halfH;
            if (i == 0) gc.moveTo(i, py);
            else gc.lineTo(i, py);
        }
        gc.stroke();
    }
}
