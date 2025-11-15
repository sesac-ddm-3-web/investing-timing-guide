package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDrawdown {
    private LocalDate startDate;
    private LocalDate bottomDate;
    private BigDecimal drawdownPercent;
    private List<RecoveryPeriod> recoveryPeriods;

    /**
     * 차트 데이터 (전중후 가격 패턴)
     */
    private ChartData chartData;
}
