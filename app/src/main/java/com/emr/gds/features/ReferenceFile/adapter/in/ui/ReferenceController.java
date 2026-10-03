package com.emr.gds.features.ReferenceFile.adapter.in.ui;

import com.emr.gds.features.ReferenceFile.application.ReferenceItem;
import com.emr.gds.features.ReferenceFile.application.ReferencePaths;
import com.emr.gds.features.ReferenceFile.application.ReferenceCsv;
import com.emr.gds.features.ReferenceFile.application.ReferenceService;
import com.emr.gds.features.ReferenceFile.ReferenceItemEditController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ReferenceController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceController.class);
    private static final int PAGE_SIZE = 200;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private Button clearSearchButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button openBaseButton;
    @FXML
    private Button importButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button backupDbButton;
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<ReferenceItem> referenceTable;
    @FXML
    private TableColumn<ReferenceItem, String> colCategory;
    @FXML
    private TableColumn<ReferenceItem, String> colContents;
    @FXML
    private TableColumn<ReferenceItem, String> colDirectoryPath;

    private ObservableList<ReferenceItem> masterData = FXCollections.observableArrayList();
    private ObservableList<ReferenceItem> filteredData = FXCollections.observableArrayList();
    private ObservableList<ReferenceItem> pageData = FXCollections.observableArrayList();

    private File basePath;
    private ReferenceService referenceService;
    private PauseTransition searchDebounce;
    private int currentPageIndex = 0;

    public void setBasePath(File basePath) {
        this.basePath = basePath;
    }

    public void setReferenceService(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colContents.setCellValueFactory(new PropertyValueFactory<>("contents"));
        colDirectoryPath.setCellValueFactory(new PropertyValueFactory<>("directoryPath"));

        referenceTable.setOnMouseClicked(this::handleTableClick);
        referenceTable.setPlaceholder(new Label("No references yet."));

        searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(e -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> searchDebounce.playFromStart());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        referenceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
        });
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        prevPageButton.setOnAction(e -> goToPage(currentPageIndex - 1));
        nextPageButton.setOnAction(e -> goToPage(currentPageIndex + 1));
        updatePaginationControls();
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            ReferenceItem selectedItem = referenceTable.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                String directoryPath = selectedItem.getDirectoryPath();
                if (directoryPath == null || directoryPath.trim().isEmpty()) {
                    showAlert("Invalid Path", "No directory configured for this reference.");
                    return;
                }
                if (!isRelativePath(directoryPath)) {
                    showAlert("Invalid Path Format", "Directory path must be strictly relative to the reference base folder.");
                    return;
                }
                File targetDirectory = resolveReferenceDirectory(directoryPath);
                if (targetDirectory != null && targetDirectory.exists() && targetDirectory.isDirectory()) {
                    openDirectoryInFileExplorer(targetDirectory);
                } else {
                    showAlert("Path Not Found", "The configured directory does not exist under the reference base folder.");
                }
            }
        }
    }

    private File resolveReferenceDirectory(String directoryPath) {
        if (this.basePath == null) {
            logger.warn("ReferenceController.basePath is not set.");
            return null;
        }

        File currentBaseDir = this.basePath;
        if (!currentBaseDir.exists() && !currentBaseDir.mkdirs()) {
            return null;
        }
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return currentBaseDir.exists() ? currentBaseDir : null;
        }

        String normalized = normalizeDirectoryPath(directoryPath);
        if (!isRelativePath(normalized)) {
            return null;
        }
        return ReferencePaths.resolve(currentBaseDir.toPath(), normalized).map(java.nio.file.Path::toFile).orElse(null);
    }


    private void openDirectoryInFileExplorer(File directory) {
        try {
            if (directory.exists() && directory.isDirectory()) {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("explorer.exe", directory.getAbsolutePath()).start();
                }
                else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    new ProcessBuilder("open", directory.getAbsolutePath()).start();
                }
                else if (System.getProperty("os.name").toLowerCase().contains("nix") || System.getProperty("os.name").toLowerCase().contains("nux")) {
                    new ProcessBuilder("xdg-open", directory.getAbsolutePath()).start();
                }
            }
        } catch (IOException e) {
            logger.error("Error opening directory in file explorer: {}", e.getMessage(), e);
            showAlert("Error", "Could not open directory in file explorer.");
        }
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_item_edit.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Reference Item");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(referenceTable.getScene().getWindow());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ReferenceItemEditController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setBasePath(basePath);
            controller.setReferenceItem(null);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                ReferenceItem newItem = controller.getReferenceItem();
                if (newItem != null) {
                    if (referenceService.existsDuplicate(newItem.getCategory(), newItem.getContents(), 0)) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Possible Duplicate");
                        confirm.setHeaderText("Similar reference already exists.");
                        confirm.setContentText("A reference with the same category and contents already exists. Save anyway?");
                        Optional<ButtonType> result = confirm.showAndWait();
                        if (result.isEmpty() || result.get() != ButtonType.OK) {
                            return;
                        }
                    }
                    referenceService.saveReference(newItem);
                    masterData.add(newItem);
                    refreshCategoryFilter();
                    applyFilters();
                    setStatus("Added reference: " + newItem.getContents());
                }
            }
        } catch (IOException | IllegalStateException e) {
            logger.error("Error opening reference item edit dialog: {}", e.getMessage(), e);
            showAlert("Error", "Could not add reference: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        ReferenceItem selectedItem = referenceTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_item_edit.fxml"));
                VBox page = loader.load();

                Stage dialogStage = new Stage();
                dialogStage.setTitle("Edit Reference Item");
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(referenceTable.getScene().getWindow());
                Scene scene = new Scene(page);
                dialogStage.setScene(scene);

                ReferenceItemEditController controller = loader.getController();
                controller.setDialogStage(dialogStage);
                controller.setBasePath(basePath);
                controller.setReferenceItem(selectedItem);

                dialogStage.showAndWait();

                if (controller.isSaveClicked()) {
                    ReferenceItem editedItem = controller.getReferenceItem();
                    if (editedItem != null) {
                        if (referenceService.existsDuplicate(editedItem.getCategory(), editedItem.getContents(), editedItem.getId())) {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle("Possible Duplicate");
                            confirm.setHeaderText("Similar reference already exists.");
                            confirm.setContentText("A reference with the same category and contents already exists. Save anyway?");
                            Optional<ButtonType> result = confirm.showAndWait();
                            if (result.isEmpty() || result.get() != ButtonType.OK) {
                                return;
                            }
                        }
                        referenceService.saveReference(editedItem);
                        masterData.set(masterData.indexOf(selectedItem), editedItem);
                        referenceTable.refresh();
                        refreshCategoryFilter();
                        applyFilters();
                        setStatus("Updated reference: " + editedItem.getContents());
                    }
                }
            } catch (IOException | IllegalStateException e) {
                logger.error("Error opening reference item edit dialog: {}", e.getMessage(), e);
                showAlert("Error", "Could not edit reference: " + e.getMessage());
            }
        } else {
            showAlert("No Selection", "Please select a reference to edit.");
        }
    }

    @FXML
    private void handleDelete() {
        ReferenceItem selectedItem = referenceTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete Reference: " + selectedItem.getContents());
            alert.setContentText("Are you sure you want to delete this reference?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    referenceService.deleteReference(selectedItem);
                } catch (IllegalStateException e) {
                    showAlert("Delete Error", e.getMessage());
                    return;
                }
                masterData.remove(selectedItem);
                refreshCategoryFilter();
                applyFilters();
                setStatus("Deleted reference: " + selectedItem.getContents());
            }
        } else {
            showAlert("No Selection", "Please select a reference to delete.");
        }
    }

    @FXML
    private void handleFind() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Find Reference");
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            logger.info("Selected file for Find: {}", file.getAbsolutePath());
            findReferencesForFile(file);
        }
    }

    private void findReferencesForFile(File file) {
        categoryFilter.getSelectionModel().select("All");
        // Reference rows describe folders; locate references for the selected file's folder.
        if (basePath != null && file.toPath().toAbsolutePath().normalize()
                .startsWith(basePath.toPath().toAbsolutePath().normalize())) {
            String relative = normalizeDirectoryPath(basePath.toPath().toAbsolutePath().normalize()
                    .relativize(file.toPath().toAbsolutePath().normalize().getParent()).toString());
            searchField.setText(relative.isEmpty() ? file.getName() : relative);
        } else {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            searchField.setText(dot > 0 ? name.substring(0, dot) : name);
        }
        applyFilters();
        setStatus("Found " + filteredData.size() + " references for: " + file.getName());
    }

    @FXML
    private void handleSave() {
        showAlert("Save Information", "Changes are automatically saved to the database.");
        setStatus("Changes are saved automatically.");
    }

    @FXML
    private void handleQuit() {
        Stage stage = (Stage) referenceTable.getScene().getWindow();
        stage.close();
    }

    public void initData() {
        if (referenceService == null) {
            logger.error("ReferenceService is not set.");
            return;
        }
        try {
            masterData.setAll(referenceService.findAllReferences());
        } catch (IllegalStateException e) {
            showAlert("Load Error", e.getMessage());
            return;
        }

        filteredData.setAll(masterData);
        referenceTable.setItems(pageData);
        refreshCategoryFilter();
        applyFilters();
        setStatus("Loaded " + masterData.size() + " references");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleClearSearch() {
        searchField.setText("");
        categoryFilter.getSelectionModel().select("All");
        applyFilters();
    }

    @FXML
    private void handleOpenBaseFolder() {
        if (basePath == null) {
            showAlert("Base Path Missing", "Reference base folder is not configured.");
            return;
        }
        openDirectoryInFileExplorer(basePath);
    }

    @FXML
    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import References CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(new Stage());
        if (file == null) {
            return;
        }

        List<ReferenceItem> importedItems = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            List<String[]> rows = parseCsv(reader);
            if (rows.isEmpty()) {
                showAlert("Import", "No rows found in CSV.");
                return;
            }
            int startIndex = 0;
            if (isHeaderRow(rows.get(0))) {
                startIndex = 1;
            }
            for (int i = startIndex; i < rows.size(); i++) {
                String[] row = rows.get(i);
                String category = row.length > 0 ? row[0] : "";
                String contents = row.length > 1 ? row[1] : "";
                String directoryPath = row.length > 2 ? row[2] : "";
                ReferenceItem item = new ReferenceItem(category, contents, normalizeDirectoryPath(directoryPath));
                importedItems.add(item);
            }
        } catch (IOException e) {
            logger.error("Failed to import CSV: {}", e.getMessage(), e);
            showAlert("Import Error", "Failed to read CSV file.");
            return;
        }

        if (importedItems.isEmpty()) {
            showAlert("Import", "No valid rows found in CSV.");
            return;
        }

        showImportPreviewDialog(importedItems);
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export References CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(new Stage());
        if (file == null) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Category,Contents,Directory Path");
            writer.newLine();
            for (ReferenceItem item : masterData) {
                writer.write(ReferenceCsv.row(item));
                writer.newLine();
            }
            setStatus("Exported " + masterData.size() + " references");
        } catch (IOException e) {
            logger.error("Failed to export CSV: {}", e.getMessage(), e);
            showAlert("Export Error", "Failed to write CSV file.");
        }
    }

    @FXML
    private void handleBackupDb() {
        File dbFile = getReferenceDbFile();
        if (dbFile == null || !dbFile.exists()) {
            showAlert("Backup Error", "Reference database file not found.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Backup Reference Database");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db"));
        File target = fileChooser.showSaveDialog(new Stage());
        if (target == null) {
            return;
        }
        try {
            Files.copy(dbFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            setStatus("Database backup saved.");
        } catch (IOException e) {
            logger.error("Failed to backup database: {}", e.getMessage(), e);
            showAlert("Backup Error", "Failed to save database backup.");
        }
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategory = categoryFilter.getSelectionModel().getSelectedItem();
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            selectedCategory = "All";
        }

        ReferenceItem selectedItem = referenceTable.getSelectionModel().getSelectedItem();
        int selectedId = selectedItem != null ? selectedItem.getId() : -1;

        filteredData.clear();
        for (ReferenceItem item : masterData) {
            String category = item.getCategory() == null ? "" : item.getCategory();
            String contents = item.getContents() == null ? "" : item.getContents();
            String directoryPath = item.getDirectoryPath() == null ? "" : item.getDirectoryPath();

            boolean matchesCategory = "All".equals(selectedCategory) || category.equals(selectedCategory);
            boolean matchesQuery = query.isEmpty() ||
                category.toLowerCase(Locale.ROOT).contains(query) ||
                contents.toLowerCase(Locale.ROOT).contains(query) ||
                directoryPath.toLowerCase(Locale.ROOT).contains(query);

            if (matchesCategory && matchesQuery) {
                filteredData.add(item);
            }
        }

        currentPageIndex = 0;
        updatePage();
        restoreSelection(selectedId);
        updatePaginationControls();
    }

    private void updatePage() {
        pageData.clear();
        int totalItems = filteredData.size();
        int totalPages = getTotalPages();
        if (currentPageIndex >= totalPages) {
            currentPageIndex = Math.max(0, totalPages - 1);
        }
        int start = currentPageIndex * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalItems);
        if (start < end) {
            pageData.addAll(filteredData.subList(start, end));
        }
        pageLabel.setText("Page " + (currentPageIndex + 1) + " / " + totalPages);
    }

    private void updatePaginationControls() {
        int totalPages = getTotalPages();
        prevPageButton.setDisable(currentPageIndex <= 0);
        nextPageButton.setDisable(currentPageIndex >= totalPages - 1);
        pageLabel.setText("Page " + (currentPageIndex + 1) + " / " + totalPages);
    }

    private void goToPage(int index) {
        int totalPages = getTotalPages();
        if (index < 0 || index >= totalPages) {
            return;
        }
        currentPageIndex = index;
        updatePage();
        updatePaginationControls();
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil(filteredData.size() / (double) PAGE_SIZE));
    }

    private void restoreSelection(int selectedId) {
        if (selectedId <= 0) {
            return;
        }
        for (ReferenceItem item : pageData) {
            if (item.getId() == selectedId) {
                referenceTable.getSelectionModel().select(item);
                return;
            }
        }
    }

    private void refreshCategoryFilter() {
        List<String> categories = referenceService.findDistinctCategories();
        for (ReferenceItem item : masterData) {
            String category = item.getCategory();
            if (category != null && !category.isEmpty() && !categories.contains(category)) {
                categories.add(category);
            }
        }
        String selectedCategory = categoryFilter.getValue();
        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All");
        categoryFilter.getItems().addAll(categories);
        categoryFilter.getSelectionModel().select(categories.contains(selectedCategory) ? selectedCategory : "All");
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private File getReferenceDbFile() {
        if (basePath != null) {
            File parent = basePath.getParentFile();
            if (parent != null) {
                return new File(parent, "references.db");
            }
        }
        return null;
    }

    private boolean isRelativePath(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        File raw = new File(normalized);
        if (raw.isAbsolute()) {
            return false;
        }
        if (normalized.startsWith("/") || normalized.startsWith("\\") || normalized.matches("^[A-Za-z]:.*")) {
            return false;
        }
        return basePath != null && ReferencePaths.resolve(basePath.toPath(), normalized).isPresent();
    }

    private String normalizeDirectoryPath(String directoryPath) {
        if (directoryPath == null) {
            return "";
        }
        String normalized = directoryPath.trim().replace("\\", "/");
        normalized = normalized.replaceAll("/+", "/");
        return normalized;
    }

    private List<String[]> parseCsv(BufferedReader reader) throws IOException {
        return ReferenceCsv.read(reader);
    }

    private boolean isHeaderRow(String[] row) {
        if (row.length < 2) {
            return false;
        }
        String c0 = row[0].toLowerCase();
        String c1 = row[1].toLowerCase();
        return c0.contains("category") && c1.contains("contents");
    }

    private void showImportPreviewDialog(List<ReferenceItem> importedItems) {
        TableView<ReferenceItem> previewTable = new TableView<>();
        TableColumn<ReferenceItem, String> c1 = new TableColumn<>("Category");
        c1.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<ReferenceItem, String> c2 = new TableColumn<>("Contents");
        c2.setCellValueFactory(new PropertyValueFactory<>("contents"));
        TableColumn<ReferenceItem, String> c3 = new TableColumn<>("Directory Path");
        c3.setCellValueFactory(new PropertyValueFactory<>("directoryPath"));
        previewTable.getColumns().addAll(c1, c2, c3);
        int previewCount = Math.min(50, importedItems.size());
        previewTable.setItems(FXCollections.observableArrayList(importedItems.subList(0, previewCount)));

        Label countLabel = new Label("Showing " + previewCount + " of " + importedItems.size() + " items.");
        Button skipButton = new Button("Skip Existing");
        Button overwriteButton = new Button("Overwrite Existing");
        Button cancelButton = new Button("Cancel");

        HBox buttons = new HBox(10, skipButton, overwriteButton, cancelButton);
        VBox root = new VBox(10, countLabel, previewTable, buttons);
        root.setPrefSize(800, 500);

        Stage dialogStage = new Stage();
        dialogStage.setTitle("Import Preview");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(referenceTable.getScene().getWindow());
        dialogStage.setScene(new Scene(root));

        skipButton.setOnAction(e -> {
            runImport(importedItems, false);
            dialogStage.close();
        });
        overwriteButton.setOnAction(e -> {
            runImport(importedItems, true);
            dialogStage.close();
        });
        cancelButton.setOnAction(e -> dialogStage.close());

        dialogStage.showAndWait();
    }

    private void runImport(List<ReferenceItem> items, boolean overwriteExisting) {
        try {
            importItems(items, overwriteExisting);
        } catch (IllegalStateException e) {
            setStatus("Import stopped because of a database error. Earlier rows may have been saved.");
            showAlert("Import Error", e.getMessage());
            try {
                reloadDataFromDb();
            } catch (IllegalStateException loadError) {
                logger.error("Could not reload references after import failure", loadError);
            }
        }
    }

    private void importItems(List<ReferenceItem> items, boolean overwriteExisting) {
        int imported = 0;
        int skipped = 0;
        for (ReferenceItem item : items) {
            String category = item.getCategory();
            String contents = item.getContents();
            String directoryPath = item.getDirectoryPath();
            if (category == null || category.isEmpty() || contents == null || contents.isEmpty()) {
                skipped++;
                continue;
            }
            if (!isRelativePath(directoryPath)) {
                skipped++;
                continue;
            }
            Optional<ReferenceItem> existing = referenceService.findByCategoryAndContents(category, contents);
            if (existing.isPresent()) {
                if (overwriteExisting) {
                    ReferenceItem existingItem = existing.get();
                    item.setId(existingItem.getId());
                    referenceService.saveReference(item);
                    imported++;
                } else {
                    skipped++;
                }
            } else {
                referenceService.saveReference(item);
                imported++;
            }
        }
        reloadDataFromDb();
        setStatus("Import complete. Imported: " + imported + ", Skipped: " + skipped);
    }

    private void reloadDataFromDb() {
        masterData.clear();
        masterData.addAll(referenceService.findAllReferences());
        refreshCategoryFilter();
        applyFilters();
    }
}
