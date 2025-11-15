package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 차트 표시를 위한 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartData {
    /**
     * 날짜 레이블 (x축)
     */
    private List<String> labels;

    /**
     * 가격 데이터 (y축)
     */
    private List<BigDecimal> prices;

    /**
     * 고점 표시용 (y축)
     */
    private BigDecimal peakPrice;

    /**
     * 고점 날짜
     */
    private String peakDate;
}
