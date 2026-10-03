package com.emr.gds.features.ReferenceFile;

import com.emr.gds.features.ReferenceFile.application.ReferenceItem;
import com.emr.gds.features.ReferenceFile.application.ReferencePaths;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class ReferenceItemEditController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField categoryField;
    @FXML
    private TextField contentsField;
    @FXML
    private TextField directoryPathField;
    @FXML
    private Button browseButton;
    @FXML
    private Label resolvedPathLabel;
    @FXML
    private Label errorLabel;

    private Stage dialogStage;
    private ReferenceItem referenceItem;
    private boolean saveClicked = false;
    private File basePath;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setBasePath(File basePath) {
        this.basePath = basePath;
        updateResolvedPathPreview();
    }

    public void setReferenceItem(ReferenceItem referenceItem) {
        this.referenceItem = referenceItem;

        if (referenceItem != null) {
            // Pre-fill fields if editing an existing item
            titleLabel.setText("Edit Reference Item");
            categoryField.setText(referenceItem.getCategory());
            contentsField.setText(referenceItem.getContents());
            directoryPathField.setText(referenceItem.getDirectoryPath());
        } else {
            // For new items
            titleLabel.setText("Add New Reference Item");
        }
        updateResolvedPathPreview();
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public ReferenceItem getReferenceItem() {
        return new ReferenceItem(referenceItem == null ? 0 : referenceItem.getId(),
                categoryField.getText(), contentsField.getText(),
                normalizeDirectoryPath(directoryPathField.getText()));
    }

    @FXML
    private void initialize() {
        directoryPathField.textProperty().addListener((obs, oldVal, newVal) -> updateResolvedPathPreview());
    }

    @FXML
    private void handleSave() {
        // Basic validation
        if (isInputValid()) {
            saveClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleBrowse() {
        if (basePath == null || !basePath.exists()) {
            showError("Reference base folder is not configured.");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Reference Directory");
        chooser.setInitialDirectory(basePath);
        File selected = chooser.showDialog(dialogStage);
        if (selected == null) {
            return;
        }
        Path base = basePath.toPath().toAbsolutePath().normalize();
        Path chosen = selected.toPath().toAbsolutePath().normalize();
        if (!chosen.startsWith(base)) {
            showError("Selected directory must be under the reference base folder.");
            return;
        }
        Path relative = base.relativize(chosen);
        directoryPathField.setText(normalizeDirectoryPath(relative.toString()));
    }

    private boolean isInputValid() {
        String errorMessage = "";
        clearFieldErrorStyles();

        if (categoryField.getText() == null || categoryField.getText().isEmpty()) {
            errorMessage += "No valid category!\n";
            markFieldInvalid(categoryField);
        }
        if (contentsField.getText() == null || contentsField.getText().isEmpty()) {
            errorMessage += "No valid contents!\n";
            markFieldInvalid(contentsField);
        }
        if (directoryPathField.getText() == null || directoryPathField.getText().isEmpty()) {
            errorMessage += "No valid directory path!\n";
            markFieldInvalid(directoryPathField);
        } else if (!isRelativePath(directoryPathField.getText())) {
            errorMessage += "Directory path must be strictly relative.\n";
            markFieldInvalid(directoryPathField);
        }

        if (errorMessage.isEmpty()) {
            errorLabel.setText("");
            return true;
        } else {
            errorLabel.setText(errorMessage.trim());
            return false;
        }
    }

    private void updateResolvedPathPreview() {
        if (resolvedPathLabel == null) {
            return;
        }
        String pathText = directoryPathField.getText();
        if (pathText == null || pathText.trim().isEmpty()) {
            resolvedPathLabel.setText("");
            return;
        }
        if (!isRelativePath(pathText)) {
            resolvedPathLabel.setText("Invalid relative path");
            return;
        }
        if (basePath == null) {
            resolvedPathLabel.setText("Base path not set");
            return;
        }
        File candidate = new File(basePath, normalizeDirectoryPath(pathText));
        resolvedPathLabel.setText(candidate.getAbsolutePath());
    }

    private String normalizeDirectoryPath(String directoryPath) {
        if (directoryPath == null) {
            return "";
        }
        String normalized = directoryPath.trim().replace("\\", "/");
        normalized = normalized.replaceAll("/+", "/");
        return normalized;
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

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void markFieldInvalid(TextField field) {
        field.setStyle("-fx-border-color: #b00020; -fx-border-width: 1;");
    }

    private void clearFieldErrorStyles() {
        categoryField.setStyle("");
        contentsField.setStyle("");
        directoryPathField.setStyle("");
    }
}
