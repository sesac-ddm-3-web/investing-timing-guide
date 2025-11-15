package com.example.demo.dto;

import com.example.demo.model.ChartData;
import com.example.demo.model.DrawdownAnalysis;
import com.example.demo.model.DrawdownLevelAnalysis;
import com.example.demo.model.HistoricalDrawdown;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAnalysisResponse {
    /**
     * 현재 하락률 정보
     */
    private DrawdownAnalysis currentDrawdown;

    /**
     * 현재 하락률과 유사한 과거 사례 (±2% 범위)
     */
    private List<HistoricalDrawdown> historicalDrawdowns;

    /**
     * 고정 하락률별 과거 사례 분석 (10%, 15%, 20%, 25%, 30%, 35%, 40%)
     */
    private List<DrawdownLevelAnalysis> drawdownLevelAnalyses;

    /**
     * 최근 1년 가격 차트 데이터
     */
    private ChartData oneYearChartData;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 데이터 시작일
     */
    private String dataStartDate;

    /**
     * 데이터 종료일 (최신 날짜)
     */
    private String dataEndDate;
}
