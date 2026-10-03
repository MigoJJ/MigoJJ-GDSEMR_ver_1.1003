package com.emr.gds.features.gout.application;

public final class GoutScoringService {

    private GoutScoringService() {
    }

    public static final String CLASSIFICATION_SOURCE = "https://doi.org/10.1002/art.39254";
    public static final String DIAGNOSIS_SOURCE = "https://doi.org/10.1136/annrheumdis-2019-215315";
    public static final String IMAGING_SOURCE = "https://doi.org/10.1136/ard-2023-224771";
    public static final String REVIEW_DATE = "2026-10-03";

    public enum CrystalResult {
        NOT_TESTED("미검사 / 결과 불명 (0점)"),
        NEGATIVE("증상 관절·점액낭의 관절액 MSU 음성 (-2점)"),
        POSITIVE_FLUID("증상 관절·점액낭의 관절액 MSU 양성"),
        POSITIVE_TOPHUS("통풍 결절 흡인물 MSU 양성");

        private final String label;
        CrystalResult(String label) { this.label = label; }
        public boolean isPositive() { return this == POSITIVE_FLUID || this == POSITIVE_TOPHUS; }
        @Override public String toString() { return label; }
    }

    public enum ClassificationStatus {
        NOT_APPLICABLE("적용 조건 미충족 — 통풍 분류 점수 적용 불가"),
        INCOMPLETE("혈청 요산 미입력 — 분류 점수 계산 불가"),
        SUFFICIENT("MSU 확인: 통풍 분류 충분 기준 충족"),
        CLASSIFIED("통풍 분류 기준 충족 (8점 이상)"),
        BELOW_THRESHOLD("통풍 분류 기준 미충족 — 통풍 배제 불가");

        private final String label;
        ClassificationStatus(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    /** Indices use the published domains; urate -1 means no measured value. */
    public record AssessmentInput(boolean entryCriterion, CrystalResult crystals,
            int jointIndex, int clinicalFeatures, int timePatternIndex,
            boolean clinicalTophus, int urateIndex, boolean ultrasoundDoubleContour,
            boolean ultrasoundTophus, boolean dectPositive, boolean erosionPositive) {
        public AssessmentInput {
            java.util.Objects.requireNonNull(crystals, "Crystal result is required");
            requireRange(jointIndex, 0, 2, "Joint pattern");
            requireRange(clinicalFeatures, 0, 3, "Clinical features");
            requireRange(timePatternIndex, 0, 2, "Time course");
            requireRange(urateIndex, -1, 4, "Serum urate");
        }
    }

    /** No numeric score is reported when the criteria do not require/allow scoring. */
    public record AssessmentResult(ClassificationStatus status, Integer score, String summary) {}

    public static AssessmentResult assess(AssessmentInput input) {
        ClassificationStatus status;
        Integer score = null;
        if (!input.entryCriterion()) status = ClassificationStatus.NOT_APPLICABLE;
        else if (input.crystals().isPositive()) status = ClassificationStatus.SUFFICIENT;
        else if (input.urateIndex() == -1) status = ClassificationStatus.INCOMPLETE;
        else {
            score = calculateScore(false, input.jointIndex(), input.clinicalFeatures(),
                    input.timePatternIndex(), input.clinicalTophus(), input.urateIndex(),
                    input.crystals() == CrystalResult.NEGATIVE,
                    input.ultrasoundDoubleContour() || input.dectPositive(), input.erosionPositive());
            status = isClassified(score) ? ClassificationStatus.CLASSIFIED : ClassificationStatus.BELOW_THRESHOLD;
        }

        StringBuilder summary = new StringBuilder("--- 통풍 분류 평가 및 진단 참고 ---\n");
        summary.append("분류 점수: 2015 ACR/EULAR; 영상 진단 참고: 2023 EULAR (2024 발표)\n");
        summary.append("적용 조건(말초 관절/점액낭의 종창·통증·압통 병력 ≥1회): ")
                .append(input.entryCriterion() ? "충족" : "미충족").append('\n');
        summary.append("MSU 검사: ").append(input.crystals()).append('\n');
        if (score != null) {
            summary.append(generateSummary(false, jointDescription(input.jointIndex()), input.clinicalFeatures(),
                    timeDescription(input.timePatternIndex()), input.clinicalTophus(), urateDescription(input.urateIndex()),
                    input.crystals() == CrystalResult.NEGATIVE,
                    input.ultrasoundDoubleContour() || input.dectPositive(), input.erosionPositive(), score));
            summary.append('\n');
        } else summary.append("분류 결과: ").append(status).append('\n');
        summary.append("초음파 이중윤곽: ").append(input.ultrasoundDoubleContour() ? "있음" : "없음/미검사")
                .append("; 초음파 결절: ").append(input.ultrasoundTophus() ? "있음" : "없음/미검사")
                .append("; DECT 요산 침착: ").append(input.dectPositive() ? "있음" : "없음/미검사").append('\n');
        if (input.ultrasoundTophus()) {
            summary.append("초음파 결절 단독 소견은 2015 영상 점수에 추가하지 않음. 임상 결절과 구분.\n");
        }
        if (input.entryCriterion()) {
            if (input.ultrasoundDoubleContour() || input.ultrasoundTophus() || input.dectPositive()) {
                summary.append("EULAR 영상 참고: 특징적 요산 침착 소견은 임상 맥락에서 통풍 진단을 뒷받침하며, ")
                        .append("진단 확인만을 위한 관절액 검사는 필수가 아님. 영상 소견은 다른 질환에서도 나타날 수 있음.\n");
            } else if (!input.crystals().isPositive()) {
                summary.append("진단 참고: 가능하면 관절액/결절의 MSU 검사를 고려. 불확실하거나 비전형적이면 초음파 또는 DECT 고려.\n");
            }
        }
        summary.append("분류 기준은 임상 진단을 대신하지 않으며, 고요산혈증만으로 통풍을 진단하지 않음.\n")
                .append("급성 관절염의 감염 등 감별은 별도 평가. MSU/영상 양성도 감염성 관절염 동반을 배제하지 않음.\n")
                .append("근거: ").append(CLASSIFICATION_SOURCE).append("; ").append(DIAGNOSIS_SOURCE)
                .append("; ").append(IMAGING_SOURCE).append(" (확인: ").append(REVIEW_DATE).append(')');
        return new AssessmentResult(status, score, summary.toString());
    }

    public static int typicalEpisodeScore(int characteristicCount, boolean recurrent) {
        requireRange(characteristicCount, 0, 3, "Time course characteristics");
        return characteristicCount >= 2 ? (recurrent ? 2 : 1) : 0;
    }

    public static String jointDescription(int index) {
        return switch (index) {
            case 0 -> "기타 관절/점액낭 또는 다관절 침범만 있음 (0점)";
            case 1 -> "발목/중족부 단·소수관절 침범, MTP1 제외 (1점)";
            case 2 -> "MTP1 단·소수관절 침범 (2점)";
            default -> throw new IllegalArgumentException("Invalid joint pattern");
        };
    }

    public static String timeDescription(int index) {
        return switch (index) {
            case 0 -> "전형적 발작 없음 (0점)";
            case 1 -> "전형적 발작 1회 (1점)";
            case 2 -> "전형적 발작 재발 (2점)";
            default -> throw new IllegalArgumentException("Invalid time course");
        };
    }

    public static String urateDescription(int index) {
        return switch (index) {
            case -1 -> "미측정 / 불명";
            case 0 -> "< 4 mg/dL (-4점)";
            case 1 -> "4 ~ < 6 mg/dL (0점)";
            case 2 -> "6 ~ < 8 mg/dL (+2점)";
            case 3 -> "8 ~ < 10 mg/dL (+3점)";
            case 4 -> "≥ 10 mg/dL (+4점)";
            default -> throw new IllegalArgumentException("Invalid serum urate range");
        };
    }

    private static void requireRange(int value, int min, int max, String name) {
        if (value < min || value > max) throw new IllegalArgumentException(name + " is outside the allowed range");
    }

    public static int calculateScore(
            boolean msuConfirmed,
            int jointIndex,
            int clinicalFeatures,
            int timePatternIndex,
            boolean tophusPresent,
            int urateIndex,
            boolean synovialFluidNegative,
            boolean imagingPositive,
            boolean erosionPositive) {

        requireRange(jointIndex, 0, 2, "Joint pattern");
        requireRange(clinicalFeatures, 0, 3, "Clinical features");
        requireRange(timePatternIndex, 0, 2, "Time course");
        requireRange(urateIndex, 0, 4, "Serum urate");
        if (msuConfirmed) {
            return Integer.MAX_VALUE;
        }

        int score = 0;
        score += jointIndex;
        score += clinicalFeatures;
        score += timePatternIndex;

        if (tophusPresent) {
            score += 4;
        }

        switch (urateIndex) {
            case 0 -> score -= 4;
            case 2 -> score += 2;
            case 3 -> score += 3;
            case 4 -> score += 4;
        }

        if (synovialFluidNegative) {
            score -= 2;
        }

        if (imagingPositive) {
            score += 4;
        }

        if (erosionPositive) {
            score += 4;
        }

        return score;
    }

    public static boolean isClassified(int score) {
        return score >= 8;
    }

    public static String generateSummary(
            boolean msuConfirmed,
            String jointDesc,
            int clinicalFeatures,
            String timePatternDesc,
            boolean tophusPresent,
            String urateDesc,
            boolean synovialFluidNegative,
            boolean imagingPositive,
            boolean erosionPositive,
            int score) {

        StringBuilder summary = new StringBuilder();
        summary.append("--- 2015 ACR/EULAR 통풍 분류 점수 상세 내역 ---\n");

        if (msuConfirmed) {
            summary.append("[확정적 기준] 증상 관절/점액낭 또는 통풍 결절 흡인물에서 요산 결정(MSU) 확인됨\n");
            summary.append("\n최종 판정: 확정적 통풍 (Sufficient Criterion 충족)");
            return summary.toString();
        }

        summary.append(String.format("- 관절 침범: %s\n", jointDesc));
        summary.append(String.format("- 임상 특징(발적, 압통, 보행장애): %d개 특징 관찰\n", clinicalFeatures));
        summary.append(String.format("- 발작 양상: %s\n", timePatternDesc));

        if (tophusPresent) {
            summary.append("- 통풍 결절(Tophus): 존재 (+4점)\n");
        }

        summary.append(String.format("- 혈청 요산 농도: %s\n", urateDesc));

        if (synovialFluidNegative) {
            summary.append("- 관절액 검사: MSU 미검출 (-2점)\n");
        }

        if (imagingPositive) {
            summary.append("- 영상(US/DECT): 요산 침착 확인 (+4점)\n");
        }

        if (erosionPositive) {
            summary.append("- 영상(X-ray): 골미란 확인 (+4점)\n");
        }

        String diagnosis = isClassified(score) ? "[통풍 분류 가능 (Gout Classified)]" : "[통풍 분류 기준 미충족 — 통풍 배제 불가 (Not Classified)]";
        summary.append("\n-----------------------------------\n");
        summary.append(String.format("총점: %d점\n최종 판정: %s", score, diagnosis));

        return summary.toString();
    }
}
