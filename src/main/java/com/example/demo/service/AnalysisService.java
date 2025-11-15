package com.example.demo.service;

import com.example.demo.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final StockDataService stockDataService;

    /**
     * Calculate current drawdown from all-time high
     */
    public DrawdownAnalysis calculateCurrentDrawdown(String ticker, List<StockData> stockDataList) {
        if (stockDataList == null || stockDataList.isEmpty()) {
            throw new IllegalArgumentException("Stock data list is empty");
        }

        // Find all-time high
        StockData peakData = stockDataList.stream()
            .max((a, b) -> a.getClose().compareTo(b.getClose()))
            .orElseThrow(() -> new RuntimeException("Failed to find peak"));

        // Get latest price (most recent date)
        StockData latestData = stockDataList.stream()
            .max((a, b) -> a.getDate().compareTo(b.getDate()))
            .orElseThrow(() -> new RuntimeException("Failed to find latest data"));

        BigDecimal currentPrice = latestData.getClose();
        BigDecimal peakPrice = peakData.getClose();

        // Calculate drawdown percentage: ((current - peak) / peak) * 100
        BigDecimal drawdown = currentPrice.subtract(peakPrice)
            .divide(peakPrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        long daysSincePeak = ChronoUnit.DAYS.between(peakData.getDate(), latestData.getDate());

        return DrawdownAnalysis.builder()
            .ticker(ticker)
            .currentPrice(currentPrice)
            .peakPrice(peakPrice)
            .peakDate(peakData.getDate())
            .drawdownPercent(drawdown.setScale(2, RoundingMode.HALF_UP))
            .daysSincePeak((int) daysSincePeak)
            .build();
    }

    /**
     * Find historical drawdowns similar to current level
     */
    public List<HistoricalDrawdown> findHistoricalDrawdowns(
            List<StockData> stockDataList,
            BigDecimal currentDrawdownPercent,
            BigDecimal tolerance) {

        List<HistoricalDrawdown> historicalDrawdowns = new ArrayList<>();

        if (stockDataList.size() < 2) {
            return historicalDrawdowns;
        }

        // Find all local peaks and their subsequent drawdowns
        for (int i = 30; i < stockDataList.size() - 30; i++) {
            StockData current = stockDataList.get(i);

            // Check if this is a local peak (highest in 30-day window)
            boolean isPeak = true;
            for (int j = Math.max(0, i - 30); j <= Math.min(stockDataList.size() - 1, i + 30); j++) {
                if (j != i && stockDataList.get(j).getClose().compareTo(current.getClose()) > 0) {
                    isPeak = false;
                    break;
                }
            }

            if (!isPeak) continue;

            // Find the bottom after this peak (within next 6 months)
            BigDecimal peakPrice = current.getClose();
            StockData bottomData = null;
            int bottomIndex = i;

            for (int j = i + 1; j < Math.min(stockDataList.size(), i + 180); j++) {
                if (bottomData == null || stockDataList.get(j).getClose().compareTo(bottomData.getClose()) < 0) {
                    bottomData = stockDataList.get(j);
                    bottomIndex = j;
                }
            }

            if (bottomData == null) continue;

            // Check if there's any price higher than peak between peak and bottom
            // If so, this is not a valid drawdown pattern
            boolean hasHigherPrice = false;
            for (int j = i + 1; j < bottomIndex; j++) {
                if (stockDataList.get(j).getClose().compareTo(peakPrice) > 0) {
                    hasHigherPrice = true;
                    break;
                }
            }

            if (hasHigherPrice) continue;

            // Calculate drawdown from this peak
            BigDecimal drawdown = bottomData.getClose().subtract(peakPrice)
                .divide(peakPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            // Check if this drawdown is similar to current (within tolerance)
            BigDecimal difference = drawdown.subtract(currentDrawdownPercent).abs();
            if (difference.compareTo(tolerance) <= 0) {
                // Calculate recovery periods (1, 3, 6, 12, 24 months)
                List<RecoveryPeriod> recoveryPeriods = calculateRecoveryPeriods(
                    stockDataList, bottomIndex, bottomData.getClose()
                );

                // Generate chart data for this historical drawdown (3 months before peak to 12 months after bottom)
                ChartData chartData = generateHistoricalDrawdownChartData(
                    stockDataList, i, bottomIndex
                );

                historicalDrawdowns.add(HistoricalDrawdown.builder()
                    .startDate(current.getDate())
                    .bottomDate(bottomData.getDate())
                    .drawdownPercent(drawdown.setScale(2, RoundingMode.HALF_UP))
                    .recoveryPeriods(recoveryPeriods)
                    .chartData(chartData)
                    .build());
            }
        }

        log.info("Found {} historical drawdowns similar to current {}%",
            historicalDrawdowns.size(), currentDrawdownPercent);

        return historicalDrawdowns;
    }

    /**
     * Calculate returns after N months from a given starting point
     */
    private List<RecoveryPeriod> calculateRecoveryPeriods(
            List<StockData> stockDataList,
            int startIndex,
            BigDecimal startPrice) {

        List<RecoveryPeriod> periods = new ArrayList<>();
        int[] monthsToCheck = {1, 3, 6, 12, 24};

        for (int months : monthsToCheck) {
            // Find data point approximately N months later
            LocalDate targetDate = stockDataList.get(startIndex).getDate().plusMonths(months);

            // Find closest date to target
            StockData closestData = null;
            long minDaysDiff = Long.MAX_VALUE;

            for (int i = startIndex; i < stockDataList.size(); i++) {
                long daysDiff = Math.abs(ChronoUnit.DAYS.between(
                    stockDataList.get(i).getDate(), targetDate
                ));

                if (daysDiff < minDaysDiff) {
                    minDaysDiff = daysDiff;
                    closestData = stockDataList.get(i);
                }

                // If we've passed the target date by more than 15 days, stop searching
                if (stockDataList.get(i).getDate().isAfter(targetDate) && daysDiff > 15) {
                    break;
                }
            }

            if (closestData != null) {
                BigDecimal returnPercent = closestData.getClose().subtract(startPrice)
                    .divide(startPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

                periods.add(RecoveryPeriod.builder()
                    .months(months)
                    .returnPercent(returnPercent)
                    .build());
            }
        }

        return periods;
    }

    /**
     * Analyze historical performance at fixed drawdown levels
     * 고정 하락률 수준별 과거 패턴 분석 (10%, 15%, 20%, 25%, 30%, 35%, 40%)
     */
    public List<DrawdownLevelAnalysis> analyzeDrawdownLevels(List<StockData> stockDataList) {
        // Define drawdown levels to analyze: -10%, -15%, -20%, -25%, -30%, -35%, -40%
        List<Integer> drawdownLevels = Arrays.asList(-10, -15, -20, -25, -30, -35, -40);

        return drawdownLevels.stream()
            .map(level -> analyzeDrawdownLevel(stockDataList, level))
            .collect(Collectors.toList());
    }

    /**
     * Analyze historical performance at a specific drawdown level
     */
    private DrawdownLevelAnalysis analyzeDrawdownLevel(List<StockData> stockDataList, int drawdownLevel) {
        // Find all historical instances where drawdown was approximately this level (±1%)
        BigDecimal targetDrawdown = BigDecimal.valueOf(drawdownLevel);
        BigDecimal tolerance = BigDecimal.valueOf(1.0);

        List<HistoricalDrawdown> historicalCases = findHistoricalDrawdowns(
            stockDataList,
            targetDrawdown,
            tolerance
        );

        // Calculate average recovery stats if we have cases
        DrawdownLevelAnalysis.AverageRecoveryStats averageStats = null;
        if (!historicalCases.isEmpty()) {
            averageStats = calculateAverageRecoveryStats(historicalCases);
        }

        // Limit to top 10 most recent cases for display
        List<HistoricalDrawdown> limitedCases = historicalCases.stream()
            .limit(10)
            .collect(Collectors.toList());

        return DrawdownLevelAnalysis.builder()
            .drawdownLevel(drawdownLevel)
            .totalCases(historicalCases.size())
            .averageStats(averageStats)
            .historicalCases(limitedCases)
            .build();
    }

    /**
     * Calculate average recovery statistics from multiple historical cases
     */
    private DrawdownLevelAnalysis.AverageRecoveryStats calculateAverageRecoveryStats(
            List<HistoricalDrawdown> cases) {

        if (cases.isEmpty()) {
            return null;
        }

        double month1Sum = 0, month3Sum = 0, month6Sum = 0, month12Sum = 0, month24Sum = 0;
        int month1Loss = 0, month3Loss = 0, month6Loss = 0, month12Loss = 0, month24Loss = 0;
        int count = cases.size();

        for (HistoricalDrawdown hd : cases) {
            List<RecoveryPeriod> periods = hd.getRecoveryPeriods();
            if (periods != null && !periods.isEmpty()) {
                for (RecoveryPeriod period : periods) {
                    double returnValue = period.getReturnPercent().doubleValue();
                    switch (period.getMonths()) {
                        case 1 -> {
                            month1Sum += returnValue;
                            if (returnValue < 0) month1Loss++;
                        }
                        case 3 -> {
                            month3Sum += returnValue;
                            if (returnValue < 0) month3Loss++;
                        }
                        case 6 -> {
                            month6Sum += returnValue;
                            if (returnValue < 0) month6Loss++;
                        }
                        case 12 -> {
                            month12Sum += returnValue;
                            if (returnValue < 0) month12Loss++;
                        }
                        case 24 -> {
                            month24Sum += returnValue;
                            if (returnValue < 0) month24Loss++;
                        }
                    }
                }
            }
        }

        return DrawdownLevelAnalysis.AverageRecoveryStats.builder()
            .month1Avg(Math.round(month1Sum / count * 100.0) / 100.0)
            .month1LossCount(month1Loss)
            .month3Avg(Math.round(month3Sum / count * 100.0) / 100.0)
            .month3LossCount(month3Loss)
            .month6Avg(Math.round(month6Sum / count * 100.0) / 100.0)
            .month6LossCount(month6Loss)
            .month12Avg(Math.round(month12Sum / count * 100.0) / 100.0)
            .month12LossCount(month12Loss)
            .month24Avg(Math.round(month24Sum / count * 100.0) / 100.0)
            .month24LossCount(month24Loss)
            .build();
    }

    /**
     * Generate chart data for a historical drawdown event
     * Shows price pattern from 3 months before peak to 12 months after bottom
     */
    private ChartData generateHistoricalDrawdownChartData(
            List<StockData> stockDataList,
            int peakIndex,
            int bottomIndex) {

        // Calculate date range: 3 months before peak to 12 months after bottom
        LocalDate peakDate = stockDataList.get(peakIndex).getDate();
        LocalDate bottomDate = stockDataList.get(bottomIndex).getDate();
        LocalDate startDate = peakDate.minusMonths(3);
        LocalDate endDate = bottomDate.plusMonths(12);

        // Filter data within this range
        List<StockData> chartDataList = stockDataList.stream()
            .filter(data -> !data.getDate().isBefore(startDate) && !data.getDate().isAfter(endDate))
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(Collectors.toList());

        if (chartDataList.isEmpty()) {
            return null;
        }

        // Extract labels and prices
        List<String> labels = chartDataList.stream()
            .map(data -> data.getDate().toString())
            .collect(Collectors.toList());

        List<BigDecimal> prices = chartDataList.stream()
            .map(StockData::getClose)
            .collect(Collectors.toList());

        // Peak price for reference
        BigDecimal peakPrice = stockDataList.get(peakIndex).getClose();

        return ChartData.builder()
            .labels(labels)
            .prices(prices)
            .peakPrice(peakPrice)
            .peakDate(peakDate.toString())
            .build();
    }

    /**
     * Generate chart data for the last year
     */
    public ChartData generateOneYearChartData(List<StockData> stockDataList) {
        if (stockDataList.isEmpty()) {
            return null;
        }

        // Get the latest date
        LocalDate latestDate = stockDataList.stream()
            .max((a, b) -> a.getDate().compareTo(b.getDate()))
            .map(StockData::getDate)
            .orElse(LocalDate.now());

        // Filter for last 365 days
        LocalDate oneYearAgo = latestDate.minusDays(365);

        List<StockData> lastYearData = stockDataList.stream()
            .filter(data -> !data.getDate().isBefore(oneYearAgo))
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(Collectors.toList());

        if (lastYearData.isEmpty()) {
            return null;
        }

        // Find peak in all data for reference
        StockData peakData = stockDataList.stream()
            .max((a, b) -> a.getClose().compareTo(b.getClose()))
            .orElse(null);

        List<String> labels = lastYearData.stream()
            .map(data -> data.getDate().toString())
            .collect(Collectors.toList());

        List<BigDecimal> prices = lastYearData.stream()
            .map(StockData::getClose)
            .collect(Collectors.toList());

        return ChartData.builder()
            .labels(labels)
            .prices(prices)
            .peakPrice(peakData != null ? peakData.getClose() : null)
            .peakDate(peakData != null ? peakData.getDate().toString() : null)
            .build();
    }

    /**
     * Get comprehensive analysis for a ticker
     */
    public com.example.demo.dto.StockAnalysisResponse analyzeStock(String ticker, int yearsBack) {
        try {
            // Get stock data
            List<StockData> stockDataList = stockDataService.getStockData(ticker, yearsBack);

            if (stockDataList.isEmpty()) {
                return com.example.demo.dto.StockAnalysisResponse.builder()
                    .message("No data available for " + ticker)
                    .build();
            }

            // Calculate current drawdown
            DrawdownAnalysis currentDrawdown = calculateCurrentDrawdown(ticker, stockDataList);

            // Find similar historical drawdowns (within 2% tolerance)
            List<HistoricalDrawdown> historicalDrawdowns = findHistoricalDrawdowns(
                stockDataList,
                currentDrawdown.getDrawdownPercent(),
                BigDecimal.valueOf(2.0)
            );

            // Analyze fixed drawdown levels (10%, 15%, 20%, etc.)
            List<DrawdownLevelAnalysis> drawdownLevelAnalyses = analyzeDrawdownLevels(stockDataList);

            // Generate 1-year chart data
            ChartData oneYearChartData = generateOneYearChartData(stockDataList);

            // Get data date range
            LocalDate startDate = stockDataList.stream()
                .map(StockData::getDate)
                .min(LocalDate::compareTo)
                .orElse(null);

            LocalDate endDate = stockDataList.stream()
                .map(StockData::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);

            return com.example.demo.dto.StockAnalysisResponse.builder()
                .currentDrawdown(currentDrawdown)
                .historicalDrawdowns(historicalDrawdowns)
                .drawdownLevelAnalyses(drawdownLevelAnalyses)
                .oneYearChartData(oneYearChartData)
                .dataStartDate(startDate != null ? startDate.toString() : null)
                .dataEndDate(endDate != null ? endDate.toString() : null)
                .message("Analysis completed successfully")
                .build();

        } catch (Exception e) {
            log.error("Error analyzing stock {}", ticker, e);
            return com.example.demo.dto.StockAnalysisResponse.builder()
                .message("Error: " + e.getMessage())
                .build();
        }
    }
}
