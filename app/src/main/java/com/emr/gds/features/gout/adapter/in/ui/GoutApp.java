package com.emr.gds.features.gout.adapter.in.ui;

import com.emr.gds.features.gout.application.GoutScoringService;
import com.emr.gds.features.gout.application.GoutScoringService.AssessmentInput;
import com.emr.gds.features.gout.application.GoutScoringService.ClassificationStatus;
import com.emr.gds.features.gout.application.GoutScoringService.CrystalResult;
import com.emr.gds.infrastructure.service.EmrBridgeService;
import javafx.application.Application;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class GoutApp extends Application {
    private final EmrBridgeService bridge = new EmrBridgeService();

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-font-family: 'Segoe UI', sans-serif;");
        Label title = note("통풍 분류 점수 · 진단 참고");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label version = note("2015 ACR/EULAR 분류 기준 + 2023 EULAR 영상 권고 (2024 발표)\n근거 확인: "
                + GoutScoringService.REVIEW_DATE);
        CheckBox entry = check("entryCriterion", "말초 관절 또는 점액낭의 종창·통증·압통 병력이 1회 이상 있음");
        ComboBox<CrystalResult> crystals = new ComboBox<>();
        crystals.setId("crystalResult");
        crystals.getItems().setAll(CrystalResult.values());
        crystals.getSelectionModel().select(CrystalResult.NOT_TESTED);
        crystals.setMaxWidth(Double.MAX_VALUE);
        VBox entrySection = section("1. 적용 조건과 충분 기준", entry,
                note("MSU 검사는 숙련된 검사자가 판독한 결과를 선택합니다. 양성이면 추가 점수 계산 없이 충분 기준을 충족합니다."), crystals);

        ComboBox<String> joint = new ComboBox<>();
        joint.setId("jointPattern");
        for (int i = 0; i < 3; i++) joint.getItems().add(GoutScoringService.jointDescription(i));
        joint.getSelectionModel().selectFirst();
        joint.setMaxWidth(Double.MAX_VALUE);
        CheckBox erythema = check("erythema", "침범 관절 위 피부 발적");
        CheckBox pressure = check("pressureIntolerance", "침범 관절에 접촉·압력을 견디기 어려움");
        CheckBox walking = check("walkingDifficulty", "보행이 매우 어렵거나 침범 관절을 사용할 수 없음");
        VBox clinicalSection = section("2. 관절 침범과 임상 특징", joint,
                note("현재뿐 아니라 과거 증상 병력을 포함하며, 관절 침범은 해당하는 가장 높은 범주 하나를 선택합니다."),
                erythema, pressure, walking);

        CheckBox rapid = check("rapidPeak", "최대 통증까지 24시간 미만");
        CheckBox resolution = check("resolutionWithin14Days", "증상이 14일 이내 소실");
        CheckBox recovery = check("completeRecovery", "발작 사이에 평소 상태로 완전히 회복");
        CheckBox recurrent = check("recurrentEpisodes", "이러한 전형적 발작이 2회 이상 재발");
        VBox timeSection = section("3. 발작 경과", note("항염증 치료 여부와 관계없이 아래 3개 중 2개 이상이면 전형적 발작입니다.\n전형적 발작 1회: 1점 / 재발: 2점."),
                rapid, resolution, recovery, recurrent);
        CheckBox clinicalTophus = check("clinicalTophus", "진찰상 전형적인 통풍 결절 존재 (+4점)");
        clinicalTophus.setTooltip(new Tooltip("전형적 부위(관절, 귀, 주두 점액낭, 손끝, 힘줄 등)의 분필 같은 피하 결절 또는 배출성 결절. 초음파 결절 단독 소견과 구분합니다."));

        ComboBox<String> urate = new ComboBox<>();
        urate.setId("serumUrate");
        for (int i = -1; i < 5; i++) urate.getItems().add(GoutScoringService.urateDescription(i));
        urate.getSelectionModel().selectFirst();
        urate.setMaxWidth(Double.MAX_VALUE);
        VBox urateSection = section("4. 혈청 요산 (필수 점수 항목)", urate,
                note("기록된 최고 요산값을 사용합니다. 이상적인 측정 조건은 요산저하제 미복용 및 발작 시작 후 4주 초과입니다.\n미측정 값을 정상으로 간주하지 않습니다. 약물 중단을 지시하는 항목이 아닙니다."));

        CheckBox doubleContour = check("ultrasoundDoubleContour", "초음파 이중윤곽 징후 — 증상 병력이 있는 관절/점액낭");
        CheckBox dect = check("dectPositive", "DECT 요산 침착 — 증상 병력이 있는 관절/점액낭");
        CheckBox ultrasoundTophus = check("ultrasoundTophus", "초음파의 특징적 통풍 결절 (영상 진단 참고)");
        CheckBox erosion = check("erosionPositive", "손/발 X-ray에서 전형적 통풍성 골미란 1개 이상 (+4점)");
        erosion.setTooltip(new Tooltip("경화성 경계와 돌출 가장자리를 동반한 피질 결손. 원위지관절 및 gull-wing 모양 미란은 제외합니다."));
        VBox imagingSection = section("5. 영상 소견", doubleContour, dect,
                note("이중윤곽 또는 DECT 양성은 합쳐서 최대 4점입니다. 아티팩트는 양성에 포함하지 않습니다."),
                ultrasoundTophus, note("초음파 결절은 2023 영상 권고의 진단 참고 소견이며, 단독으로 2015 영상 점수를 추가하지 않습니다."), erosion);

        Button calculate = new Button("평가 및 점수 계산");
        calculate.setId("calculateGout");
        calculate.setStyle("-fx-base: #2ecc71; -fx-font-weight: bold;");
        Label result = note("결과: 적용 조건과 검사 결과를 입력한 뒤 계산하세요.");
        result.setId("goutResult");
        TextArea summary = new TextArea();
        summary.setId("goutSummary");
        summary.setEditable(false);
        summary.setWrapText(true);
        summary.setPrefHeight(260);
        Button copy = new Button("결과 복사");
        copy.setId("copyGout");
        Button save = new Button("EMR에 저장");
        save.setId("saveGout");
        copy.setDisable(true);
        save.setDisable(true);
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(summary.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        save.setOnAction(e -> {
            if (bridge.insertBlock(5, "\n" + summary.getText() + "\n")) {
                new Alert(Alert.AlertType.INFORMATION, "EMR에 성공적으로 저장되었습니다.").showAndWait();
                stage.close();
            } else {
                new Alert(Alert.AlertType.ERROR, "EMR을 찾을 수 없습니다. 메인 창이 열려 있는지 확인해 주세요.").showAndWait();
            }
        });
        calculate.setOnAction(e -> {
            int features = count(erythema, pressure, walking);
            int time = GoutScoringService.typicalEpisodeScore(count(rapid, resolution, recovery), recurrent.isSelected());
            var assessment = GoutScoringService.assess(new AssessmentInput(entry.isSelected(), crystals.getValue(),
                    joint.getSelectionModel().getSelectedIndex(), features, time, clinicalTophus.isSelected(),
                    urate.getSelectionModel().getSelectedIndex() - 1, doubleContour.isSelected(),
                    ultrasoundTophus.isSelected(), dect.isSelected(), erosion.isSelected()));
            result.setText((assessment.score() == null ? "" : "총점: " + assessment.score() + "/23점 — ") + assessment.status());
            summary.setText(assessment.summary());
            boolean complete = assessment.status() != ClassificationStatus.NOT_APPLICABLE
                    && assessment.status() != ClassificationStatus.INCOMPLETE;
            copy.setDisable(!complete);
            save.setDisable(!complete);
        });

        Observable[] inputs = {entry.selectedProperty(), crystals.valueProperty(), joint.valueProperty(),
                erythema.selectedProperty(), pressure.selectedProperty(), walking.selectedProperty(),
                rapid.selectedProperty(), resolution.selectedProperty(), recovery.selectedProperty(), recurrent.selectedProperty(),
                clinicalTophus.selectedProperty(), urate.valueProperty(), doubleContour.selectedProperty(),
                ultrasoundTophus.selectedProperty(), dect.selectedProperty(), erosion.selectedProperty()};
        for (Observable input : inputs) input.addListener(observable -> {
            summary.clear();
            result.setText("입력이 변경되었습니다. 다시 계산하세요.");
            copy.setDisable(true);
            save.setDisable(true);
        });

        VBox sources = section("근거 자료",
                sourceLink("2015 ACR/EULAR 분류 기준", GoutScoringService.CLASSIFICATION_SOURCE),
                sourceLink("2018 EULAR 통풍 진단 권고 (2020 발표)", GoutScoringService.DIAGNOSIS_SOURCE),
                sourceLink("2023 EULAR 영상 권고 (2024 발표)", GoutScoringService.IMAGING_SOURCE));
        TitledPane evidence = new TitledPane("근거와 기준 버전", sources);
        evidence.setExpanded(false);
        root.getChildren().addAll(title, version, entrySection, clinicalSection, timeSection, clinicalTophus,
                urateSection, imagingSection, calculate, result, summary, new HBox(10, copy, save), evidence);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        stage.setTitle("Gout — 통풍 분류 및 진단 참고");
        stage.setScene(new Scene(scroll, 720, 850));
        stage.show();
    }

    private Hyperlink sourceLink(String title, String url) {
        Hyperlink link = new Hyperlink(title);
        link.setOnAction(e -> getHostServices().showDocument(url));
        return link;
    }

    private static int count(CheckBox... boxes) {
        int count = 0;
        for (CheckBox box : boxes) if (box.isSelected()) count++;
        return count;
    }

    private static CheckBox check(String id, String text) {
        CheckBox box = new CheckBox(text);
        box.setId(id);
        box.setWrapText(true);
        return box;
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static VBox section(String title, javafx.scene.Node... children) {
        Label heading = note(title);
        heading.setStyle("-fx-font-weight: bold;");
        VBox section = new VBox(6, heading);
        section.getChildren().addAll(children);
        return section;
    }

    public static void main(String[] args) { launch(args); }
}
