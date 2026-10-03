package com.emr.gds.features.gout.application;

import com.emr.gds.features.gout.application.GoutScoringService.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoutAssessmentTest {
    private AssessmentInput input(boolean entry, CrystalResult crystals, int urate,
            boolean doubleContour, boolean ultrasoundTophus, boolean dect) {
        return new AssessmentInput(entry, crystals, 0, 0, 0, false, urate,
                doubleContour, ultrasoundTophus, dect, false);
    }

    @Test
    void entryCriterionIsRequiredEvenForHighScoresOrCrystals() {
        var result = GoutScoringService.assess(new AssessmentInput(false, CrystalResult.NOT_TESTED,
                2, 3, 2, true, 4, true, true, true, true));
        assertEquals(ClassificationStatus.NOT_APPLICABLE, result.status());
        assertNull(result.score());
        assertEquals(ClassificationStatus.NOT_APPLICABLE,
                GoutScoringService.assess(input(false, CrystalResult.POSITIVE_FLUID, -1, false, false, false)).status());
    }

    @Test
    void fluidAndTophusCrystalsAreSufficientWithoutSerumUrate() {
        for (CrystalResult crystals : new CrystalResult[]{CrystalResult.POSITIVE_FLUID, CrystalResult.POSITIVE_TOPHUS}) {
            var result = GoutScoringService.assess(input(true, crystals, -1, false, false, false));
            assertEquals(ClassificationStatus.SUFFICIENT, result.status());
            assertNull(result.score());
            assertFalse(result.summary().contains("2147483647"));
        }
    }

    @Test
    void missingUrateIsNotAssumedNormal() {
        var result = GoutScoringService.assess(input(true, CrystalResult.NOT_TESTED, -1, true, false, false));
        assertEquals(ClassificationStatus.INCOMPLETE, result.status());
        assertNull(result.score());
        assertTrue(result.summary().contains("계산 불가"));
    }

    @Test
    void allFiveUrateCategoriesHavePublishedWeights() {
        int[] points = {-4, 0, 2, 3, 4};
        for (int i = 0; i < points.length; i++) {
            assertEquals(points[i], GoutScoringService.assess(input(true, CrystalResult.NOT_TESTED, i, false, false, false)).score());
        }
    }

    @Test
    void onlyConfirmedNegativeFluidSubtractsTwo() {
        assertEquals(-6, GoutScoringService.assess(input(true, CrystalResult.NEGATIVE, 0, false, false, false)).score());
        assertEquals(-4, GoutScoringService.assess(input(true, CrystalResult.NOT_TESTED, 0, false, false, false)).score());
    }

    @Test
    void multipleImagingModalitiesDoNotDoubleCount() {
        assertEquals(4, GoutScoringService.assess(input(true, CrystalResult.NOT_TESTED, 1, true, false, true)).score());
        assertEquals(0, GoutScoringService.assess(input(true, CrystalResult.NOT_TESTED, 1, false, true, false)).score());
        assertTrue(GoutScoringService.assess(input(true, CrystalResult.NOT_TESTED, 1, false, true, false)).summary()
                .contains("진단 확인만을 위한 관절액 검사는 필수가 아님"));
    }

    @Test
    void sevenIsBelowAndEightMeetsClassificationThreshold() {
        var seven = GoutScoringService.assess(new AssessmentInput(true, CrystalResult.NOT_TESTED,
                2, 2, 1, false, 2, false, false, false, false));
        var eight = GoutScoringService.assess(new AssessmentInput(true, CrystalResult.NOT_TESTED,
                2, 3, 1, false, 2, false, false, false, false));
        assertEquals(7, seven.score());
        assertEquals(ClassificationStatus.BELOW_THRESHOLD, seven.status());
        assertTrue(seven.summary().contains("통풍 배제 불가"));
        assertFalse(seven.summary().contains("통풍 아님"));
        assertEquals(8, eight.score());
        assertEquals(ClassificationStatus.CLASSIFIED, eight.status());
    }

    @Test
    void maximumScoreRemains23() {
        var result = GoutScoringService.assess(new AssessmentInput(true, CrystalResult.NOT_TESTED,
                2, 3, 2, true, 4, true, true, true, true));
        assertEquals(23, result.score());
    }

    @Test
    void typicalEpisodeNeedsAtLeastTwoTimeCharacteristics() {
        assertEquals(0, GoutScoringService.typicalEpisodeScore(1, true));
        assertEquals(1, GoutScoringService.typicalEpisodeScore(2, false));
        assertEquals(2, GoutScoringService.typicalEpisodeScore(2, true));
        assertEquals(2, GoutScoringService.typicalEpisodeScore(3, true));
    }

    @Test
    void invalidDomainValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AssessmentInput(true, CrystalResult.NOT_TESTED,
                3, 0, 0, false, 1, false, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> GoutScoringService.calculateScore(false,
                0, 4, 0, false, 1, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> GoutScoringService.typicalEpisodeScore(-1, false));
    }
}
