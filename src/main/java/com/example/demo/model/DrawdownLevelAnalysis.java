package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 특정 하락률 수준에서의 과거 사례 분석
 * 예: -10%, -15%, -20% 등 고정된 하락률에서의 과거 패턴
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawdownLevelAnalysis {

    /**
     * 하락률 수준 (예: -10, -15, -20)
     */
    private int drawdownLevel;

    /**
     * 이 하락률을 경험한 과거 사례 수
     */
    private int totalCases;

    /**
     * 과거 사례들의 평균 수익률
     */
    private AverageRecoveryStats averageStats;

    /**
     * 개별 과거 사례 목록 (최대 10개만 표시)
     */
    private List<HistoricalDrawdown> historicalCases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AverageRecoveryStats {
        /**
         * 1개월 후 평균 수익률
         */
        private double month1Avg;
        private int month1LossCount;

        /**
         * 3개월 후 평균 수익률
         */
        private double month3Avg;
        private int month3LossCount;

        /**
         * 6개월 후 평균 수익률
         */
        private double month6Avg;
        private int month6LossCount;

        /**
         * 12개월 후 평균 수익률
         */
        private double month12Avg;
        private int month12LossCount;

        /**
         * 24개월 후 평균 수익률
         */
        private double month24Avg;
        private int month24LossCount;
    }
}
