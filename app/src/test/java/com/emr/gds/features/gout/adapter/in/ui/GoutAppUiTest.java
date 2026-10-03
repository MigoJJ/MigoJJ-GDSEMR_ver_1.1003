package com.emr.gds.features.gout.adapter.in.ui;

import com.emr.gds.features.gout.application.GoutScoringService.CrystalResult;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class GoutAppUiTest {
    private Scene scene;

    @Start
    void start(Stage stage) {
        new GoutApp().start(stage);
        scene = stage.getScene();
    }

    private CheckBox check(String id) { return (CheckBox) scene.lookup("#" + id); }
    private Button button(String id) { return (Button) scene.lookup("#" + id); }
    private String result() { return ((Label) scene.lookup("#goutResult")).getText(); }
    private String summary() { return ((TextArea) scene.lookup("#goutSummary")).getText(); }
    @SuppressWarnings("unchecked")
    private <T> ComboBox<T> combo(String id) { return (ComboBox<T>) scene.lookup("#" + id); }

    @Test
    void refusesClassificationWithoutSymptomsAndWithoutUrate(FxRobot robot) {
        robot.interact(() -> button("calculateGout").fire());
        assertTrue(result().contains("적용 조건 미충족"));
        assertTrue(button("saveGout").isDisabled());
        robot.interact(() -> { check("entryCriterion").setSelected(true); button("calculateGout").fire(); });
        assertTrue(result().contains("혈청 요산 미입력"));
        assertTrue(button("copyGout").isDisabled());
    }

    @Test
    void tophusCrystalConfirmationDoesNotRequireUrateOrNumericScore(FxRobot robot) {
        robot.interact(() -> {
            check("entryCriterion").setSelected(true);
            this.<CrystalResult>combo("crystalResult").setValue(CrystalResult.POSITIVE_TOPHUS);
            button("calculateGout").fire();
        });
        assertTrue(result().contains("충분 기준 충족"));
        assertFalse(result().contains("총점"));
        assertTrue(summary().contains("결절 흡인물"));
        assertFalse(button("saveGout").isDisabled());
    }

    @Test
    void belowThresholdNeverLabelsPatientAsNotHavingGout(FxRobot robot) {
        robot.interact(() -> {
            check("entryCriterion").setSelected(true);
            combo("serumUrate").getSelectionModel().select(2); // 4 to <6
            button("calculateGout").fire();
        });
        assertTrue(result().contains("0/23점"));
        assertTrue(summary().contains("통풍 배제 불가"));
        assertFalse(summary().contains("통풍 아님"));
    }

    @Test
    void ultrasoundTophusIsDiagnosticContextWithoutExtraClassificationPoints(FxRobot robot) {
        robot.interact(() -> {
            check("entryCriterion").setSelected(true);
            combo("serumUrate").getSelectionModel().select(2);
            check("ultrasoundTophus").setSelected(true);
            button("calculateGout").fire();
        });
        assertTrue(result().contains("0/23점"));
        assertTrue(summary().contains("2023 EULAR"));
        assertTrue(summary().contains("임상 결절과 구분"));
    }

    @Test
    void maximumScoreAndChangedInputInvalidateSavedResult(FxRobot robot) {
        robot.interact(() -> {
            check("entryCriterion").setSelected(true);
            combo("jointPattern").getSelectionModel().select(2);
            combo("serumUrate").getSelectionModel().select(5);
            for (String id : new String[]{"erythema", "pressureIntolerance", "walkingDifficulty",
                    "rapidPeak", "resolutionWithin14Days", "completeRecovery", "recurrentEpisodes",
                    "clinicalTophus", "ultrasoundDoubleContour", "dectPositive", "erosionPositive"}) {
                check(id).setSelected(true);
            }
            button("calculateGout").fire();
        });
        assertTrue(result().contains("23/23점"));
        assertFalse(button("saveGout").isDisabled());
        robot.interact(() -> check("clinicalTophus").setSelected(false));
        assertTrue(summary().isEmpty());
        assertTrue(button("copyGout").isDisabled());
        assertTrue(button("saveGout").isDisabled());
    }
}
