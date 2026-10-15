package com.emr.gds.features.glp1.adapter.in.ui;

import com.emr.gds.core.db.DbPaths;
import com.emr.gds.input.IAIFxTextAreaManager;
import com.emr.gds.input.IAIMain;
import com.emr.gds.input.IAITextAreaManager;
import com.emr.gds.util.StageSizing;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Glp1SemaglutideMain extends Application {

    private static final String INSURANCE_REFERENCE =
            "references/guidelines/Ozempic® (semaglutide) 한국 건강보험 급여 기준.odt";

    @Override
    public void start(Stage stage) {
        Glp1SemaglutidePane medPane = new Glp1SemaglutidePane();

        TextArea txtOutput = new TextArea();
        txtOutput.setPromptText("Problem-list output will appear here...");
        txtOutput.setPrefRowCount(8);
        txtOutput.setWrapText(true);

        Button btnExport = new Button("Generate to EMR");
        Button btnSave   = new Button("Save to EMR");
        Button btnClear  = new Button("Clear All");
        Button btnQuit   = new Button("Quit");
        Button btnInsurance = new Button("오젬픽 건강보험 급여 기준");
        btnInsurance.setId("ozempicInsuranceReference");
        btnInsurance.setOnAction(e -> openInsuranceReference(stage, btnInsurance));

        btnExport.setOnAction(e -> txtOutput.setText(medPane.toProblemListString()));
        btnClear.setOnAction(e -> {
            medPane.clearAll();
            txtOutput.clear();
        });
        btnSave.setOnAction(e -> {
            String content = txtOutput.getText().trim();
            if (content.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Nothing to save. Generate text first.").showAndWait();
                return;
            }

            IAIMain.getManagerSafely().ifPresentOrElse(
                    manager -> {
                        String textToAppend = content.endsWith("\n") ? content : content + "\n";
                        manager.appendTextToSection(IAITextAreaManager.AREA_PI, textToAppend);

                        String assessmentLine = medPane.toAssessmentSummary();
                        if (!assessmentLine.isBlank()) {
                            manager.appendTextToSection(IAIFxTextAreaManager.AREA_A, assessmentLine + "\n");
                        }
                    },
                    () -> new Alert(
                            Alert.AlertType.ERROR,
                            "EMR mainframe is not connected.\nOpen this tool from the EMR to enable saving."
                    ).showAndWait()
            );
        });
        btnQuit.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(10, btnExport, btnSave, btnClear, btnQuit);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(6));

        BorderPane bottomPane = new BorderPane();
        bottomPane.setTop(new Separator());
        bottomPane.setCenter(txtOutput);
        bottomPane.setBottom(buttonBox);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("glp1-window");
        HBox referenceBox = new HBox(btnInsurance);
        referenceBox.setPadding(new Insets(10, 12, 0, 12));
        root.setTop(referenceBox);
        root.setCenter(medPane);
        root.setBottom(bottomPane);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/glp1.css").toExternalForm());
        stage.setTitle("GLP-1RA (Semaglutide) - EMR Module Demo");
        stage.setScene(scene);
        StageSizing.fitToScreen(stage, 0.5, 0.9);
        stage.show();
    }

    private void openInsuranceReference(Stage owner, Button button) {
        Task<Void> openTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                openReferenceDocument(DbPaths.resolveDbPath(INSURANCE_REFERENCE));
                return null;
            }
        };
        button.setDisable(true);
        openTask.setOnSucceeded(e -> button.setDisable(false));
        openTask.setOnFailed(e -> {
            button.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "급여 기준 문서를 열지 못했습니다.\n" + openTask.getException().getMessage());
            alert.setTitle("오젬픽 급여 기준 문서 열기 오류");
            alert.initOwner(owner);
            alert.show();
        });
        Thread worker = new Thread(openTask, "ozempic-reference-opener");
        worker.setDaemon(true);
        worker.start();
    }

    protected void openReferenceDocument(Path document) throws IOException, InterruptedException {
        if (!Files.isRegularFile(document)) {
            throw new IOException("문서 파일을 찾을 수 없습니다: " + document.toAbsolutePath());
        }
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        // Keep Linux document opening out of AWT/GTK and off the JavaFX thread.
        if (os.contains("linux") || os.contains("mac")) {
            String launcher = os.contains("mac") ? "open" : "xdg-open";
            Process process = new ProcessBuilder(launcher, document.toAbsolutePath().toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            // Some launchers stay alive while the document is open; do not wait indefinitely.
            if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() != 0) {
                throw new IOException(launcher + " 실행 실패 (종료 코드 " + process.exitValue()
                        + "). ODT 기본 연결 프로그램을 확인해 주세요.");
            }
        } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(document.toFile());
        } else {
            throw new IOException("이 환경에서는 기본 문서 프로그램을 실행할 수 없습니다.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
