package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawdownAnalysis {
    private String ticker;
    private BigDecimal currentPrice;
    private BigDecimal peakPrice;
    private LocalDate peakDate;
    private BigDecimal drawdownPercent;
    private int daysSincePeak;
}
