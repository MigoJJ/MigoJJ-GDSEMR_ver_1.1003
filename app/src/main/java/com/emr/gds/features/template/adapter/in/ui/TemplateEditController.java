package com.emr.gds.features.template.adapter.in.ui;

import com.emr.gds.features.template.application.TemplateModel;
import com.emr.gds.features.template.application.TemplateSectionService;
import com.emr.gds.features.template.persistence.TemplateRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class TemplateEditController {

    @FXML private TableView<TemplateModel> templateTable;
    @FXML private TableColumn<TemplateModel, String> nameColumn;
    @FXML private TextField nameField;
    @FXML private TextArea contentArea;

    private TemplateRepository repository;
    private TemplateSectionService sectionService;
    private Consumer<String> onUseCallback;
    private TemplateModel selectedTemplate;

    public void setRepository(TemplateRepository repository) {
        this.repository = repository;
        loadTemplates();
    }

    public void setSectionService(TemplateSectionService service) {
        this.sectionService = service;
    }

    public void setOnUseCallback(Consumer<String> callback) {
        this.onUseCallback = callback;
    }

    @FXML
    public void initialize() {
        if (sectionService == null) {
            sectionService = new TemplateSectionService();
        }

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        Collator nameCollator = Collator.getInstance(Locale.KOREAN);
        nameCollator.setStrength(Collator.SECONDARY);
        nameColumn.setComparator(Comparator.nullsFirst(nameCollator::compare));

        templateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectTemplate(newVal);
            }
        });
    }

    private void loadTemplates() {
        if (repository == null) return;
        List<TemplateModel> templates = repository.getAllTemplates();
        ObservableList<TemplateModel> data = FXCollections.observableArrayList(templates);
        templateTable.setItems(data);
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);
        templateTable.getSortOrder().setAll(nameColumn);
        templateTable.sort();
    }

    private void selectTemplate(TemplateModel template) {
        this.selectedTemplate = template;
        nameField.setText(template.getName());
        contentArea.setText(template.getContent());
    }

    @FXML
    private void handleNew() {
        templateTable.getSelectionModel().clearSelection();
        selectedTemplate = null;
        nameField.clear();
        contentArea.clear();
        nameField.requestFocus();
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String content = contentArea.getText();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Template name cannot be empty.");
            return;
        }

        if (repository == null) return;

        if (selectedTemplate == null) {
            repository.createTemplate(name, content);
        } else {
            repository.updateTemplate(selectedTemplate.getId(), name, content);
        }

        loadTemplates();
        handleNew();
    }

    @FXML
    private void handleDelete() {
        if (selectedTemplate == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a template to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText(null);
        alert.setContentText("Delete this template?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            repository.deleteTemplate(selectedTemplate.getId());
            loadTemplates();
            handleNew();
        }
    }

    @FXML
    private void handleUse() {
        String rawContent = contentArea.getText();
        if (rawContent.isEmpty()) return;

        LinkedHashMap<String, List<String>> sections = sectionService.parseSections(rawContent);
        String finalOutput = sectionService.buildOrderedOutput(sections);

        if (onUseCallback != null) {
            onUseCallback.accept(finalOutput);
        }

        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
