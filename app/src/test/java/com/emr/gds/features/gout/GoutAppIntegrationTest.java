package com.emr.gds.features.gout;

import com.emr.gds.features.gout.application.GoutScoringService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoutAppIntegrationTest {

    @Test
    void testSufficientCriterionShortCircuits() {
        int score = GoutScoringService.calculateScore(
                true,  // msuConfirmed
                0, 0, 0, false, 0, false, false, false
        );

        assertEquals(Integer.MAX_VALUE, score);
    }

    @Test
    void testScoreCalculationWithVariousCombinations() {
        // Low score scenario
        int lowScore = GoutScoringService.calculateScore(
                false, 0, 0, 0, false, 1, false, false, false
        );
        assertTrue(lowScore < 8);

        // Medium score
        int mediumScore = GoutScoringService.calculateScore(
                false, 2, 2, 1, false, 2, false, false, false
        );
        assertTrue(mediumScore >= 0 && mediumScore < 15);

        // High score
        int highScore = GoutScoringService.calculateScore(
                false, 2, 3, 2, true, 4, false, true, true
        );
        assertTrue(highScore >= 8);
    }

    @Test
    void testClassificationThreshold() {
        assertFalse(GoutScoringService.isClassified(7));
        assertTrue(GoutScoringService.isClassified(8));
        assertTrue(GoutScoringService.isClassified(100));
    }

    @Test
    void testSummaryWithAllFactors() {
        String summary = GoutScoringService.generateSummary(
                false,
                "엄지발가락 MTP1 (2점)",
                3,
                "재발성 전형적 발작 (2점)",
                true,
                "≥ 10 (4점)",
                false,
                true,
                true,
                23
        );

        assertTrue(summary.contains("관절 침범"));
        assertTrue(summary.contains("임상 특징"));
        assertTrue(summary.contains("발작 양상"));
        assertTrue(summary.contains("통풍 결절"));
        assertTrue(summary.contains("혈청 요산"));
        assertTrue(summary.contains("영상"));
        assertTrue(summary.contains("골미란"));
        assertTrue(summary.contains("총점: 23점"));
        assertTrue(summary.contains("통풍 분류 가능"));
    }

    @Test
    void testSummaryWithMinimalSelection() {
        String summary = GoutScoringService.generateSummary(
                false,
                "해당 없음 (0점)",
                0,
                "없음 (0점)",
                false,
                "4 ~ < 6 (0점)",
                false,
                false,
                false,
                0
        );

        assertTrue(summary.contains("총점: 0점"));
        assertTrue(summary.contains("통풍 분류 기준 미충족"));
        assertTrue(summary.contains("통풍 배제 불가"));
    }


    @Test
    void testMSUConfirmedSummary() {
        String summary = GoutScoringService.generateSummary(
                true,
                "",
                0,
                "",
                false,
                "",
                false,
                false,
                false,
                Integer.MAX_VALUE
        );

        assertTrue(summary.contains("MSU"));
        assertTrue(summary.contains("확정적 통풍"));
        assertTrue(summary.contains("Sufficient Criterion"));
    }
}
