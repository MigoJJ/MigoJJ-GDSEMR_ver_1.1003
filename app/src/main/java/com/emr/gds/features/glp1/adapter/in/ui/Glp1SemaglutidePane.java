package com.emr.gds.features.glp1.adapter.in.ui;

import com.emr.gds.features.glp1.application.Glp1FormatterService;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import java.time.LocalDate;

public class Glp1SemaglutidePane extends VBox {

    private final CheckBox chkOnTherapy  = new CheckBox("On therapy");
    private final CheckBox chkForT2dm    = new CheckBox("Indication: T2DM");
    private final CheckBox chkForObesity = new CheckBox("Indication: Obesity / OW");
    private final CheckBox chkForAscvd   = new CheckBox("Indication: ASCVD risk lower");

    private final ComboBox<String> cmbBrand = new ComboBox<>();
    private final ComboBox<String> cmbDose  = new ComboBox<>();
    private final TextField txtDoseCustom   = new TextField();

    private final DatePicker dpNextFollowUp             = new DatePicker();
    private final ComboBox<String> cmbFollowUpInterval  = new ComboBox<>();
    private final TextArea txtFollowUpNotes             = new TextArea();

    private final CheckBox chkMtcMen2      = new CheckBox("Personal/family hx MTC or MEN2");
    private final CheckBox chkPancreatitis = new CheckBox("Hx pancreatitis (caution)");
    private final CheckBox chkPregnancy    = new CheckBox("Pregnancy / planning pregnancy");
    private final CheckBox chkSevereGi     = new CheckBox("Severe GI disease / gastroparesis");
    private final TextArea txtOtherContra  = new TextArea();

    private final Label lblValidation = new Label();

    public Glp1SemaglutidePane() {
        setSpacing(8);
        setPadding(new Insets(10));
        getStyleClass().add("glp1-semaglutide-pane");

        Label lblHeader = new Label("MEDICATION - GLP-1RA (SEMAGLUTIDE)");
        lblHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        TitledPane paneChecklist = createChecklistPane();
        TitledPane paneDose      = createDosePane();
        TitledPane paneFollowUp  = createFollowUpPane();
        TitledPane paneContra    = createContraindicationsPane();

        lblValidation.setStyle("-fx-text-fill: #d9534f; -fx-font-size: 11px;");
        lblValidation.setWrapText(true);

        setupEventListeners();

        VBox content = new VBox(8, lblHeader, paneChecklist, paneDose, paneFollowUp, paneContra, lblValidation);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-padding: 0;");

        getChildren().add(scrollPane);

        if (dpNextFollowUp.getValue() == null) {
            dpNextFollowUp.setValue(LocalDate.now());
        }
    }

    private TitledPane createChecklistPane() {
        VBox box = new VBox(6, chkOnTherapy, chkForT2dm, chkForObesity, chkForAscvd);
        box.setPadding(new Insets(8));

        TitledPane pane = new TitledPane("Checklist / Indication", box);
        pane.setExpanded(true);
        pane.setCollapsible(true);
        return pane;
    }

    private TitledPane createDosePane() {
        cmbBrand.getItems().setAll(
                "Ozempic (semaglutide, SC weekly)",
                "Wegovy (semaglutide, SC weekly)",
                "Rybelsus (semaglutide, PO daily)",
                "Mounjaro (tirzepatide, SC weekly)"
        );
        cmbBrand.setPromptText("Select brand / route...");
        cmbBrand.setPrefWidth(260);

        cmbDose.setPromptText("Select dose...");
        cmbDose.setPrefWidth(250);

        cmbBrand.setOnAction(e -> {
            cmbDose.getItems().clear();
            cmbDose.getSelectionModel().clearSelection();

            String brand = cmbBrand.getValue();
            if (brand == null) {
                return;
            }

            if (brand.startsWith("Ozempic")) {
                cmbDose.getItems().setAll(
                        "0.25 mg weekly",
                        "0.5 mg weekly",
                        "1.0 mg weekly",
                        "2.0 mg weekly"
                );
            } else if (brand.startsWith("Wegovy")) {
                cmbDose.getItems().setAll(
                        "0.25 mg weekly",
                        "0.5 mg weekly",
                        "1.0 mg weekly",
                        "1.7 mg weekly",
                        "2.4 mg weekly"
                );
            } else if (brand.startsWith("Rybelsus")) {
                cmbDose.getItems().setAll(
                        "3 mg PO daily",
                        "7 mg PO daily",
                        "14 mg PO daily"
                );
            } else if (brand.startsWith("Mounjaro")) {
                cmbDose.getItems().setAll(
                        "2.5 mg weekly (initiation, 4 wks)",
                        "5.0 mg weekly",
                        "7.5 mg weekly",
                        "10.0 mg weekly",
                        "12.5 mg weekly",
                        "15.0 mg weekly (max)"
                );
            }
        });

        txtDoseCustom.setPromptText("Custom dose (e.g., 1.5 mg weekly)");
        txtDoseCustom.setPrefWidth(250);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 2;");

        Label lblBrand = new Label("Brand / Route:");
        lblBrand.setStyle("-fx-font-weight: bold;");
        grid.add(lblBrand, 0, 0);
        grid.add(cmbBrand, 1, 0);

        Label lblDose = new Label("Dose (preset):");
        lblDose.setStyle("-fx-font-weight: bold;");
        grid.add(lblDose, 0, 1);
        grid.add(cmbDose, 1, 1);

        Label lblCustom = new Label("Dose (custom):");
        lblCustom.setStyle("-fx-font-weight: bold;");
        grid.add(lblCustom, 0, 2);
        grid.add(txtDoseCustom, 1, 2);

        ColumnConstraints col0 = new ColumnConstraints(100, 120, Double.MAX_VALUE);
        ColumnConstraints col1 = new ColumnConstraints(250, 250, Double.MAX_VALUE);
        grid.getColumnConstraints().addAll(col0, col1);

        TitledPane pane = new TitledPane("Dose", grid);
        pane.setExpanded(true);
        pane.setCollapsible(true);
        return pane;
    }

    private TitledPane createFollowUpPane() {
        cmbFollowUpInterval.getItems().addAll(
                "Every 4 weeks",
                "Every 8 weeks",
                "Every 12 weeks",
                "PRN / as needed"
        );
        cmbFollowUpInterval.setPromptText("Select interval...");
        cmbFollowUpInterval.setPrefWidth(250);

        txtFollowUpNotes.setPromptText("Follow-up notes (e.g., monitor weight, HbA1c, GI symptoms...)");
        txtFollowUpNotes.setPrefRowCount(4);
        txtFollowUpNotes.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 2;");

        Label lblDate = new Label("Next visit:");
        lblDate.setStyle("-fx-font-weight: bold;");
        grid.add(lblDate, 0, 0);
        grid.add(dpNextFollowUp, 1, 0);

        Label lblInterval = new Label("Interval:");
        lblInterval.setStyle("-fx-font-weight: bold;");
        grid.add(lblInterval, 0, 1);
        grid.add(cmbFollowUpInterval, 1, 1);

        Label lblNotes = new Label("Notes:");
        lblNotes.setStyle("-fx-font-weight: bold;");
        GridPane.setValignment(lblNotes, VPos.TOP);
        grid.add(lblNotes, 0, 2);
        grid.add(txtFollowUpNotes, 1, 2);

        ColumnConstraints col0 = new ColumnConstraints(100, 120, Double.MAX_VALUE);
        ColumnConstraints col1 = new ColumnConstraints(250, 300, Double.MAX_VALUE);
        grid.getColumnConstraints().addAll(col0, col1);

        TitledPane pane = new TitledPane("Follow-up", grid);
        pane.setExpanded(true);
        pane.setCollapsible(true);
        return pane;
    }

    private TitledPane createContraindicationsPane() {
        VBox checks = new VBox(6, chkMtcMen2, chkPancreatitis, chkPregnancy, chkSevereGi);

        txtOtherContra.setPromptText("Other contraindications / cautions...");
        txtOtherContra.setPrefRowCount(3);
        txtOtherContra.setWrapText(true);

        VBox box = new VBox(10, checks, txtOtherContra);
        box.setPadding(new Insets(8));
        box.setStyle(
                "-fx-border-color: #ffe6e6; " +
                "-fx-border-radius: 2; " +
                "-fx-background-color: #fff9f9;"
        );

        TitledPane pane = new TitledPane("Contraindications / Cautions", box);
        pane.setExpanded(true);
        pane.setCollapsible(true);
        return pane;
    }

    private void setupEventListeners() {
        chkOnTherapy.selectedProperty().addListener((obs, oldVal, newVal) -> validateForm());
        chkMtcMen2.selectedProperty().addListener((obs, oldVal, newVal) -> validateForm());
        chkPregnancy.selectedProperty().addListener((obs, oldVal, newVal) -> validateForm());
    }

    private void validateForm() {
        String warnings = Glp1FormatterService.getValidationWarning(
                chkOnTherapy.isSelected(),
                chkMtcMen2.isSelected(),
                chkPregnancy.isSelected(),
                cmbBrand.getValue()
        );
        lblValidation.setText(warnings);
    }

    public String toProblemListString() {
        return Glp1FormatterService.formatProblemList(
                chkOnTherapy.isSelected(),
                chkForT2dm.isSelected(),
                chkForObesity.isSelected(),
                chkForAscvd.isSelected(),
                cmbBrand.getValue(),
                cmbDose.getValue(),
                txtDoseCustom.getText().trim(),
                dpNextFollowUp.getValue(),
                cmbFollowUpInterval.getValue(),
                txtFollowUpNotes.getText().trim(),
                chkMtcMen2.isSelected(),
                chkPancreatitis.isSelected(),
                chkPregnancy.isSelected(),
                chkSevereGi.isSelected(),
                txtOtherContra.getText().trim()
        );
    }

    public String toAssessmentSummary() {
        return Glp1FormatterService.formatAssessmentSummary(
                cmbBrand.getValue(),
                cmbDose.getValue(),
                txtDoseCustom.getText().trim(),
                dpNextFollowUp.getValue()
        );
    }

    public void clearAll() {
        chkOnTherapy.setSelected(false);
        chkForT2dm.setSelected(false);
        chkForObesity.setSelected(false);
        chkForAscvd.setSelected(false);

        cmbBrand.setValue(null);
        cmbDose.setValue(null);
        txtDoseCustom.clear();

        dpNextFollowUp.setValue(LocalDate.now());
        cmbFollowUpInterval.setValue(null);
        txtFollowUpNotes.clear();

        chkMtcMen2.setSelected(false);
        chkPancreatitis.setSelected(false);
        chkPregnancy.setSelected(false);
        chkSevereGi.setSelected(false);
        txtOtherContra.clear();

        lblValidation.setText("");
    }

    public boolean hasCriticalContraindications() {
        return chkMtcMen2.isSelected() || chkPregnancy.isSelected();
    }
}
