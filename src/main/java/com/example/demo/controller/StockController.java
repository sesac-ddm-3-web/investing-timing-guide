package com.example.demo.controller;

import com.example.demo.dto.StockAnalysisResponse;
import com.example.demo.service.AnalysisService;
import com.example.demo.service.StockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StockController {

    private final StockDataService stockDataService;
    private final AnalysisService analysisService;

    private static final List<String> DEFAULT_TICKERS = Arrays.asList("QQQ", "VOO", "SOXX");
    private static final int DEFAULT_YEARS = 2;  // 2년치 데이터면 충분

    /**
     * Get analysis for a specific stock
     * GET /api/stocks/{ticker}/analysis?years=10
     */
    @GetMapping("/{ticker}/analysis")
    public ResponseEntity<StockAnalysisResponse> getStockAnalysis(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "10") int years) {

        log.info("Analyzing {} with {} years of data", ticker, years);
        StockAnalysisResponse response = analysisService.analyzeStock(ticker.toUpperCase(), years);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh stock data (disabled - using local CSV data only)
     * POST /api/stocks/{ticker}/refresh
     */
    @PostMapping("/{ticker}/refresh")
    public ResponseEntity<Map<String, String>> refreshStockData(
            @PathVariable String ticker) {

        log.info("Refresh requested for {} - not supported (using local data)", ticker);
        return ResponseEntity.ok(Map.of(
            "status", "info",
            "message", "Refresh not available - using local CSV data. Please update CSV files in history/ folder and run convert_csv_to_json.py"
        ));
    }

    /**
     * Get list of default supported tickers
     * GET /api/stocks/supported
     */
    @GetMapping("/supported")
    public ResponseEntity<List<String>> getSupportedTickers() {
        return ResponseEntity.ok(DEFAULT_TICKERS);
    }

    /**
     * Get analysis for all default tickers
     * GET /api/stocks/analysis/all?years=10
     */
    @GetMapping("/analysis/all")
    public ResponseEntity<Map<String, StockAnalysisResponse>> getAllAnalysis(
            @RequestParam(defaultValue = "10") int years) {

        log.info("Analyzing all default tickers");
        Map<String, StockAnalysisResponse> results = new java.util.HashMap<>();

        for (String ticker : DEFAULT_TICKERS) {
            try {
                StockAnalysisResponse response = analysisService.analyzeStock(ticker, years);
                results.put(ticker, response);
            } catch (Exception e) {
                log.error("Error analyzing {}", ticker, e);
                results.put(ticker, StockAnalysisResponse.builder()
                    .message("Error: " + e.getMessage())
                    .build());
            }
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Initialize all stock data (disabled - using local CSV data only)
     * POST /api/stocks/initialize
     * To initialize data, run convert_csv_to_json.py script
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeAllStockData() {

        log.info("Initialize requested - not supported (using local data)");
        Map<String, Object> results = new java.util.HashMap<>();

        results.put("status", "info");
        results.put("message", "Initialization not available - using local CSV data");
        results.put("instructions", "To update data: 1) Update CSV files in history/ folder, 2) Run: python3 convert_csv_to_json.py");
        results.put("supportedTickers", DEFAULT_TICKERS);

        return ResponseEntity.ok(results);
    }

    /**
     * Health check endpoint
     * GET /api/stocks/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Stock Investment Guide"
        ));
    }
}
