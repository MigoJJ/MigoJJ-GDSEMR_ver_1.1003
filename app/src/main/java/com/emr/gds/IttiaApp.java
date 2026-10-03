package com.emr.gds;

import com.emr.gds.core.auth.CredentialRepository;
import com.emr.gds.core.auth.SqliteCredentialRepository;
import com.emr.gds.core.db.AppDatabaseManager;
import com.emr.gds.repository.sqlite.SqliteAbbreviationRepository;
import com.emr.gds.repository.sqlite.SqlitePlanHistoryRepository;
import com.emr.gds.repository.sqlite.SqliteProblemRepository;
import com.emr.gds.features.ReferenceFile.persistence.SqliteReferenceRepository;
import com.emr.gds.service.AbbreviationService;
import com.emr.gds.service.PlanHistoryService;
import com.emr.gds.service.ProblemListService;
import com.emr.gds.features.ReferenceFile.application.ReferenceService;
import com.emr.gds.features.imaging.adapter.in.ui.ChestXrayReviewStage;
import com.emr.gds.features.ekg.adapter.in.ui.EkgReportStage;
import com.emr.gds.features.ekg.adapter.in.ui.EkgSimpleReportApp;
import com.emr.gds.features.ekg.adapter.in.ui.EkgQuickInterpreter;
import com.emr.gds.features.gout.adapter.in.ui.GoutApp;
import com.emr.gds.features.allergy.adapter.in.ui.AllergyApp;
import com.emr.gds.features.bone.adapter.in.ui.DexaRiskAssessmentApp;
import com.emr.gds.input.IAIFreqFrame;
import com.emr.gds.input.IAIFxTextAreaManager;
import com.emr.gds.input.IAIMain;
import com.emr.gds.input.IAITextAreaManager;
import com.emr.gds.shared.ui.IAMButtonAction;
import com.emr.gds.shared.ui.IAMFunctionkey;
import com.emr.gds.shared.ui.IAMProblemAction;
import com.emr.gds.shared.ui.IAMTextArea;
import com.emr.gds.shared.ui.IAMTextFormatUtil;
import com.emr.gds.shared.ui.TextAreaControlProcessor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import com.emr.gds.features.medication.adapter.in.ui.MedicationCategory;
import com.emr.gds.features.thyroid.adapter.in.ui.ThyroidLauncher;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Main JavaFX Application for GDSEMR ITTIA - EMR Prototype.
 * This class serves as the entry point for the application and is responsible for:
 * - Initializing the main application window.
 * - Setting up the user interface, including toolbars, text areas, and panels.
 * - Managing database connections and loading initial data.
 * - Handling user interactions, such as button clicks and keyboard shortcuts.
 * - Coordinating communication between different UI components and managers.
 */
public class IttiaApp extends Application {

    // ================================
    // Constants
    // ================================
    private static final String APP_TITLE = " GDSEMRITTIA  EMR prototype(JAVAFX)";
    private static final int SCENE_WIDTH = 1350;
    private static final int SCENE_HEIGHT = 1000;
    private static final int INITIAL_FOCUS_AREA = 0; // Corresponds to the first text area

    // ================================
    // UI and Core Logic Components
    // ================================
    private IAMProblemAction problemAction;
    private IAMButtonAction buttonAction;
    private IAMTextArea textAreaManager;
    private final Map<String, String> abbrevMap = new HashMap<>();
    private final AbbreviationService abbreviationService =
            new AbbreviationService(new SqliteAbbreviationRepository(), abbrevMap);
    private final ProblemListService problemListService =
            new ProblemListService(new SqliteProblemRepository());
    private final PlanHistoryService planHistoryService =
            new PlanHistoryService(new SqlitePlanHistoryRepository());
    private final CredentialRepository credentialRepository = new SqliteCredentialRepository();
    private IAIFreqFrame freqStage; // Manages the vital signs window
    private IAMFunctionkey functionKeyHandler;
    private Stage mainStage;
    
    // Profiling
    private long startTime;

    // ================================
    // Application Lifecycle
    // ================================

    /**
     * Main entry point for the JavaFX application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes and starts the primary stage of the application.
     * @param primaryStage The primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        this.startTime = System.currentTimeMillis();
        this.mainStage = primaryStage;
        primaryStage.setTitle(APP_TITLE);

        showLoginScene(primaryStage);
        System.out.println("Time to first window: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void showLoginScene(Stage stage) {
        Scene loginScene = buildLoginScene();
        stage.setScene(loginScene);
        stage.show();
    }

    private Scene buildLoginScene() {
        boolean setupMode = !credentialRepository.hasCredential();

        BorderPane shell = new BorderPane();
        shell.setPadding(new Insets(40, 32, 40, 32));
        shell.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b);");

        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 18; -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.35), 24, 0, 0, 12);");

        Label badge = new Label("GDSEMR");
        badge.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label title = new Label(setupMode ? "Create a password" : "Sign in to continue");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label(setupMode
                ? "No password is set yet. Choose one to secure this EMR workspace."
                : "Secure access to the EMR workspace.");
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 14px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefWidth(360);
        usernameField.setStyle("-fx-background-radius: 12; -fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.55);");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(setupMode ? "New password (min 6 characters)" : "Password");
        passwordField.setPrefWidth(360);
        passwordField.setStyle("-fx-background-radius: 12; -fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.55);");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm password");
        confirmPasswordField.setPrefWidth(360);
        confirmPasswordField.setStyle("-fx-background-radius: 12; -fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.55);");

        Button loginButton = new Button(setupMode ? "Create Password" : "Sign In");
        loginButton.setDefaultButton(true);
        loginButton.setPrefWidth(160);
        loginButton.setStyle("-fx-background-radius: 12; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: linear-gradient(to right, #4f46e5, #06b6d4);");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #facc15; -fx-font-size: 12px;");

        PasswordField effectiveConfirmField = setupMode ? confirmPasswordField : null;
        loginButton.setOnAction(e -> handleLogin(usernameField, passwordField, effectiveConfirmField, setupMode, statusLabel, loginButton));
        passwordField.setOnAction(e -> handleLogin(usernameField, passwordField, effectiveConfirmField, setupMode, statusLabel, loginButton));
        confirmPasswordField.setOnAction(e -> handleLogin(usernameField, passwordField, effectiveConfirmField, setupMode, statusLabel, loginButton));

        VBox buttonRow = new VBox(loginButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.setPadding(new Insets(8, 0, 0, 0));

        card.getChildren().add(badge);
        card.getChildren().add(title);
        card.getChildren().add(subtitle);
        card.getChildren().add(usernameField);
        card.getChildren().add(passwordField);
        if (setupMode) {
            card.getChildren().add(confirmPasswordField);
        }
        card.getChildren().add(buttonRow);
        card.getChildren().add(statusLabel);

        StackPane center = new StackPane(card);
        center.setAlignment(Pos.CENTER);
        shell.setCenter(center);

        return new Scene(shell, 900, 600);
    }

    private void handleLogin(TextField usernameField, PasswordField passwordField, PasswordField confirmPasswordField,
                              boolean setupMode, Label statusLabel, Button loginButton) {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getText().toCharArray();

        if (username.isEmpty() || password.length == 0) {
            statusLabel.setText("Enter both username and password.");
            return;
        }

        if (setupMode) {
            if (password.length < 6) {
                statusLabel.setText("Password must be at least 6 characters.");
                return;
            }
            char[] confirm = confirmPasswordField.getText().toCharArray();
            if (!java.util.Arrays.equals(password, confirm)) {
                statusLabel.setText("Passwords do not match.");
                java.util.Arrays.fill(confirm, '\0');
                return;
            }
            java.util.Arrays.fill(confirm, '\0');
            credentialRepository.setPassword(password);
        } else {
            if (!credentialRepository.verifyPassword(password)) {
                statusLabel.setText("Invalid password.");
                java.util.Arrays.fill(password, '\0');
                return;
            }
        }
        java.util.Arrays.fill(password, '\0');

        statusLabel.setText("Signing in & loading data...");
        loginButton.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        if (confirmPasswordField != null) {
            confirmPasswordField.setDisable(true);
        }

        // Load data asynchronously
        javafx.concurrent.Task<Map<String, String>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, String> call() throws Exception {
                return abbreviationService.loadAll();
            }
        };

        loadTask.setOnSucceeded(e -> {
            launchMainScene(loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            statusLabel.setText("Error loading data: " + loadTask.getException().getMessage());
            loginButton.setDisable(false);
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            if (confirmPasswordField != null) {
                confirmPasswordField.setDisable(false);
            }
            loadTask.getException().printStackTrace();
        });

        new Thread(loadTask).start();
    }

    private void launchMainScene(Map<String, String> loadedAbbreviations) {
        try {
            // Initialize core components before building the UI
            initializeApplicationComponents(loadedAbbreviations);
            
            // Build the main layout
            BorderPane root = buildRootLayout();
            root.getStyleClass().add("font-small");
            Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            
            // Load the CSS stylesheet
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            mainStage.setTitle(APP_TITLE);
            mainStage.setScene(scene);
            mainStage.show();
            
            // Perform setup tasks after the stage is visible
            configurePostShow(scene);
            
            System.out.println("Time to main scene: " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception e) {
            showFatalError("Application Startup Error", "Failed to start the application.", e);
        }
    }

    public Stage getPrimaryStage() {
        return mainStage;
    }

    /**
     * Cleans up resources when the application is closed.
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        // Ensure background tasks and database connections are closed
        if (problemAction != null) {
            problemAction.closeResources();
        }
        AppDatabaseManager.getInstance().closeAll();
        System.out.println("All database connections closed.");
    }

    // ================================
    // Initialization Methods
    // ================================

    /**
     * Initializes database connection and core application managers.
     */
    private void initializeApplicationComponents(Map<String, String> loadedAbbreviations) throws IOException, ClassNotFoundException, SQLException {
        // Load Data
        if (loadedAbbreviations != null && loadedAbbreviations != abbrevMap) {
            abbrevMap.clear();
            abbrevMap.putAll(loadedAbbreviations);
        }
        
        problemAction = new IAMProblemAction(this, problemListService);
        textAreaManager = new IAMTextArea(abbrevMap, problemAction, abbreviationService, planHistoryService);
        buttonAction = new IAMButtonAction(this, abbreviationService);
        textAreaManager.setAssessmentDoubleClickHandler((textArea, index) -> buttonAction.openKcd9Manager());
        functionKeyHandler = new IAMFunctionkey(this);
    }

    // ================================
    // UI Layout Methods
    // ================================

    /**
     * Constructs the root BorderPane layout for the main scene.
     */
    private BorderPane buildRootLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        root.setTop(buildTopToolBar());
        root.setLeft(buildLeftPanel());
        root.setCenter(buildCenterPanel());
        root.setBottom(buildBottomPanel());

        // Establish the connection bridge for inter-component communication
        establishBridgeConnection();

        return root;
    }

    /**
     * Builds the top toolbar with action buttons.
     */
    private ToolBar buildTopToolBar() {
        ToolBar topBar = buttonAction.buildTopBar();

        // Create and configure additional buttons
        Button templateButton = new Button("Load Template");
        templateButton.setOnAction(e -> openTemplateEditor());

        Button vitalButton = new Button("Vital BP & HbA1c");
        vitalButton.setOnAction(e -> openVitalWindow());
        
        // --- Diagnostic Tools (Accented) ---
        Button dexaButton = new Button("DEXA");
        dexaButton.getStyleClass().add("button-accent");
        dexaButton.setOnAction(e -> {
            DexaRiskAssessmentApp.open();
        });
        
        Button ekgButton = new Button("EKG");
        ekgButton.getStyleClass().add("button-accent");
        ekgButton.setOnAction(e -> EkgReportStage.open());
        
        Button ekgQuickButton = new Button("EKG Quick");
        ekgQuickButton.getStyleClass().add("button-accent");
        ekgQuickButton.setOnAction(e -> EkgQuickInterpreter.open());
        
        Button cpaButton = new Button("Chest X-ray");
        cpaButton.getStyleClass().add("button-accent");
        cpaButton.setOnAction(event -> {
            ChestXrayReviewStage chestPAWindow = new ChestXrayReviewStage(mainStage);
            chestPAWindow.show();
        });

        Button goutButton = new Button("Gout");
        goutButton.getStyleClass().add("button-accent");
        goutButton.setOnAction(e -> {
            new GoutApp().start(new Stage());
        });

        Button categoryButton = new Button("Category");
        categoryButton.setOnAction(e -> {
            try {
                new MedicationCategory().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        // Add buttons to the toolbar
        topBar.getItems().addAll(
            new Separator(), templateButton,
            new Separator(), vitalButton,
            new Separator(), categoryButton,
            new Separator(), 
            new Label("Diagnostics:"), // Group Label
            dexaButton, ekgButton, ekgQuickButton, cpaButton, goutButton
        );
        return topBar;
    }

    /**
     * Builds the left panel containing the problem list.
     */
    private VBox buildLeftPanel() {
        VBox leftPanel = problemAction.buildProblemPane();
        BorderPane.setMargin(leftPanel, new Insets(0, 10, 0, 0));
        return leftPanel;
    }

    /**
     * Builds the center panel with the main EMR text areas.
     */
    private GridPane buildCenterPanel() {
        GridPane centerPane = textAreaManager.buildCenterAreas();
        centerPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #fdfbf7, #fbf8f1);"); // Ecru gradient
        return centerPane;
    }

    /**
     * Builds the bottom toolbar.
     */
    private ToolBar buildBottomPanel() {
        try {
            ToolBar bottomBar = buttonAction.buildBottomBar();

            Button categoryButton = new Button("Category");
            categoryButton.getStyleClass().add("button-accent");
            categoryButton.setOnAction(e -> {
                try {
                    new MedicationCategory().start(new Stage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            Button thyroidButton = new Button("Thyroid");
            thyroidButton.getStyleClass().add("button-accent");
            thyroidButton.setOnAction(e -> ThyroidLauncher.openThyroidEmr());

            Button thyroidPregButton = new Button("Thyroid Pregnancy");
            thyroidPregButton.getStyleClass().add("button-accent");
            thyroidPregButton.setOnAction(e -> ThyroidLauncher.openThyroidPregnancy());

            Button allergyButton = new Button("Allergy");
            allergyButton.getStyleClass().add("button-accent");
            allergyButton.setOnAction(e -> {
                try {
                    new AllergyApp().start(new Stage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            Button labCodeButton = new Button("LabCode");
            labCodeButton.getStyleClass().add("button-accent");
            labCodeButton.setOnAction(e -> com.emr.gds.features.clinicalLab.adapter.in.ui.ClinicalLabLauncher.open());
            
            bottomBar.getItems().add(new Separator());
            bottomBar.getItems().add(categoryButton);
            bottomBar.getItems().add(new Separator());
            bottomBar.getItems().add(thyroidButton);
            bottomBar.getItems().add(thyroidPregButton);
            bottomBar.getItems().add(new Separator());
            bottomBar.getItems().add(allergyButton);
            bottomBar.getItems().add(new Separator());
            bottomBar.getItems().add(labCodeButton);
            bottomBar.getItems().add(new Separator());

            Button referenceButton = new Button("Reference");
            referenceButton.getStyleClass().add("button-accent");
            referenceButton.setOnAction(e -> openReferenceManager());

            bottomBar.getItems().add(referenceButton);
            
            return bottomBar;
        } catch (Exception e) {
            System.err.println("Error building bottom panel: " + e.getMessage());
            e.printStackTrace();
            // Provide a fallback UI in case of an error
            ToolBar fallbackToolBar = new ToolBar();
            Label errorLabel = new Label("Error loading bottom panel");
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
            fallbackToolBar.getItems().add(errorLabel);
            return fallbackToolBar;
        }
    }

    // ================================
    // Window Management
    // ================================

    /**
     * Opens or focuses the vital signs window.
     */
    public void openVitalWindow() {
        if (!isBridgeReady()) {
            showToast("Text areas not ready yet. Please try again in a moment.");
            return;
        }

        // Use a singleton pattern for the vital signs window
        if (freqStage == null || !freqStage.isShowing()) {
            freqStage = new IAIFreqFrame();
        } else {
            freqStage.requestFocus();
            freqStage.toFront();
        }
    }

    /**
     * Opens the EMR template editor.
     */
    private void openTemplateEditor() {
        com.emr.gds.features.template.adapter.in.ui.TemplateEditStage.open(templateContent ->
            textAreaManager.parseAndAppendTemplate(templateContent)
        );
    }

    /**
     * Opens the Reference Manager window.
     */
    private void openReferenceManager() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_frame.fxml"));
            Stage referenceStage = new Stage();
            referenceStage.setTitle("Reference Manager");
            referenceStage.setScene(new Scene(loader.load()));
            
            // Get the controller and inject the base path and service
            com.emr.gds.features.ReferenceFile.adapter.in.ui.ReferenceController controller = loader.getController();
            File referenceBasePath = getRepoRoot().resolve("app").resolve("db").resolve("references").toFile();
            controller.setBasePath(referenceBasePath);
            
            // Instantiate and inject ReferenceService
            SqliteReferenceRepository referenceRepository = new SqliteReferenceRepository(AppDatabaseManager.getInstance());
            ReferenceService referenceService = new ReferenceService(referenceRepository);
            controller.setReferenceService(referenceService);
            
            controller.initData();

            referenceStage.setResizable(false);
            referenceStage.show();
        } catch (IOException | IllegalStateException e) {
            showFatalError("Reference Manager Error", "Failed to open Reference Manager.", e);
        }
    }

    // ================================
    // Post-Initialization Setup
    // ================================

    /**
     * Configures the application after the main stage is shown.
     */
    private void configurePostShow(Scene scene) {
        TextAreaControlProcessor.installGlobalProcessor(abbrevMap);

        Platform.runLater(() -> {
            // Ensure the bridge is ready and set initial focus
            if (!isBridgeReady()) {
                establishBridgeConnection();
            }
            textAreaManager.focusArea(INITIAL_FOCUS_AREA);
        });
        installAllKeyboardShortcuts(scene);
    }

    /**
     * Establishes a static bridge to allow external components (like Swing windows)
     * to interact with the JavaFX text areas.
     */
    private void establishBridgeConnection() {
        var areas = textAreaManager.getInternalTextAreas();
        if (areas == null || areas.isEmpty()) {
            throw new IllegalStateException("EMR text areas not initialized. buildCenterAreas() must run first.");
        }
        // Set the global static manager for external access
        IAIMain.setTextAreaManager(new IAIFxTextAreaManager(areas));
    }

    /**
     * Checks if the text area bridge is ready for interaction.
     */
    private boolean isBridgeReady() {
        return Optional.ofNullable(IAIMain.getTextAreaManager())
                       .map(IAITextAreaManager::isReady)
                       .orElse(false);
    }

    // ================================
    // Keyboard Shortcuts
    // ================================

    /**
     * Installs all keyboard shortcuts for the application.
     */
    private void installAllKeyboardShortcuts(Scene scene) {
        installGlobalKeyboardShortcuts(scene);
        functionKeyHandler.installFunctionKeyShortcuts(scene);
    }

    /**
     * Installs global shortcuts like date insertion, formatting, and copying.
     */
    private void installGlobalKeyboardShortcuts(Scene scene) {
        Map<KeyCombination, Runnable> shortcuts = new HashMap<>();

        // Ctrl+I: Insert current date
        shortcuts.put(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN), () -> {
            String currentDateString = " [ " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " ]";
            insertLineIntoFocusedArea(currentDateString);
        });
        
        // Ctrl+Shift+F: Format current text area
        shortcuts.put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::formatCurrentArea);
        
        // Ctrl+Shift+C: Copy all content to clipboard
        shortcuts.put(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::copyAllToClipboard);

        addAreaFocusShortcuts(shortcuts);

        // Register all shortcuts with the scene
        shortcuts.forEach((keyCombination, action) -> scene.getAccelerators().put(keyCombination, action));
    }

    /**
     * Adds shortcuts (Ctrl+1 through Ctrl+0) to focus specific text areas.
     */
    private void addAreaFocusShortcuts(Map<KeyCombination, Runnable> shortcuts) {
        for (int i = 1; i <= 9; i++) {
            final int areaIndex = i - 1;
            shortcuts.put(new KeyCodeCombination(KeyCode.getKeyCode(String.valueOf(i)), KeyCombination.CONTROL_DOWN),
                          () -> textAreaManager.focusArea(areaIndex));
        }
        // Ctrl+0 focuses the 10th area
        shortcuts.put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN), () -> textAreaManager.focusArea(9));
    }

    // ================================
    // Text Manipulation Methods
    // ================================

    public void insertTemplateIntoFocusedArea(IAMButtonAction.TemplateLibrary template) {
        textAreaManager.insertTemplateIntoFocusedArea(template);
    }

    public void insertLineIntoFocusedArea(String line) {
        textAreaManager.insertLineIntoFocusedArea(line);
    }

    public void insertBlockIntoFocusedArea(String block) {
        textAreaManager.insertBlockIntoFocusedArea(block);
    }

    public void formatCurrentArea() {
        textAreaManager.formatCurrentArea();
    }

    public void clearAllText() {
        textAreaManager.clearAllTextAreas();
        Optional.ofNullable(problemAction).ifPresent(IAMProblemAction::clearScratchpad);
    }

    // ================================
    // Clipboard Operations
    // ================================

    /**
     * Compiles all EMR content, formats it, and copies it to the system clipboard.
     */
    public void copyAllToClipboard() {
        String compiledContent = compileAllContent();
        String finalizedContent = IAMTextFormatUtil.finalizeForEMR(compiledContent);

        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(finalizedContent);
        Clipboard.getSystemClipboard().setContent(clipboardContent);

        showToast("Copied all content to clipboard");
    }

    /**
     * Gathers content from the problem list and all text areas.
     */
    private String compileAllContent() {
        StringJoiner contentJoiner = new StringJoiner("\n\n");
        addProblemListToContent(contentJoiner);
        addTextAreasToContent(contentJoiner);
        return contentJoiner.toString();
    }

    /**
     * Appends the formatted problem list to the content joiner.
     */
    private void addProblemListToContent(StringJoiner contentJoiner) {
        Optional.ofNullable(problemAction)
                .map(IAMProblemAction::getProblems)
                .filter(problems -> !problems.isEmpty())
                .ifPresent(problems -> {
                    StringBuilder problemBuilder = new StringBuilder("# Problem List (as of ")
                            .append(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                            .append(")\n");
                    problems.forEach(problem -> problemBuilder.append("- ").append(problem).append("\n"));
                    contentJoiner.add(problemBuilder.toString().trim());
                });
    }

    /**
     * Appends content from each text area to the content joiner.
     */
    private void addTextAreasToContent(StringJoiner contentJoiner) {
        List<TextArea> textAreas = Optional.ofNullable(textAreaManager)
                                           .map(IAMTextArea::getTextAreas)
                                           .orElse(List.of());

        for (int i = 0; i < textAreas.size(); i++) {
            String uniqueText = IAMTextFormatUtil.getUniqueLines(textAreas.get(i).getText());
            if (!uniqueText.isEmpty()) {
                String title = getAreaTitle(i);
                contentJoiner.add("# " + title + "\n" + uniqueText);
            }
        }
    }

    /**
     * Retrieves the title for a given text area index.
     */
    private String getAreaTitle(int areaIndex) {
        return (areaIndex < IAMTextArea.TEXT_AREA_TITLES.length)
                ? IAMTextArea.TEXT_AREA_TITLES[areaIndex].replaceAll(">$", "")
                : "Area " + (areaIndex + 1);
    }

    // ================================
    // Utility Methods
    // ================================

    /**
     * Finds the root directory of the repository.
     */
    private Path getRepoRoot() {
        Path p = Paths.get("").toAbsolutePath();
        // Traverse up until a marker file is found
        while (p != null && !Files.exists(p.resolve("gradlew")) && !Files.exists(p.resolve(".git"))) {
            p = p.getParent();
        }
        return (p != null) ? p : Paths.get("").toAbsolutePath();
    }

    /**
     * Constructs the full path to a database file within the project structure.
     */
    private Path getDbPath(String fileName) {
        return getRepoRoot().resolve("app").resolve("db").resolve(fileName);
    }

    /**
     * Displays a simple informational pop-up message.
     */
    private void showToast(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Info");
        alert.showAndWait();
    }

    /**
     * Displays a fatal error message and exits the application.
     */
    private void showFatalError(String title, String message, Throwable cause) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("A fatal error occurred.");
        alert.setContentText(message + "\n\nDetails: " + cause.getMessage());
        
        // Add expandable stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        cause.printStackTrace(pw);
        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        alert.getDialogPane().setExpandableContent(textArea);
        
        alert.showAndWait();
        Platform.exit();
    }

    // ================================
    // Getters for Component Access
    // ================================

    public void applyTextAreaTheme(IAMTextArea.Theme theme) {
        if (textAreaManager != null) {
            textAreaManager.setTheme(theme);
        }
    }

    public IAMTextArea getTextAreaManager() {
        return textAreaManager;
    }

    public Map<String, String> getAbbrevMap() {
        return abbrevMap;
    }

    public IAMFunctionkey getFunctionKeyHandler() {
        return functionKeyHandler;
    }
}
