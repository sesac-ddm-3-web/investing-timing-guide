package com.example.demo.service;

import com.example.demo.model.StockData;
import com.example.demo.repository.JsonDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataService {

    private final JsonDataRepository jsonDataRepository;

    /**
     * Get stock data from local JSON file
     * @param ticker Stock ticker symbol (e.g., "QQQ", "VOO", "SOXX")
     * @param yearsBack Not used (kept for API compatibility)
     */
    public List<StockData> getStockData(String ticker, int yearsBack) {
        log.info("Loading local data for {}", ticker);

        if (!jsonDataRepository.hasData(ticker)) {
            log.error("No data file found for {}. Please run convert_csv_to_json.py first", ticker);
            throw new RuntimeException("No data file found for " + ticker +
                ". Please run convert_csv_to_json.py to generate JSON data from CSV files.");
        }

        List<StockData> data = jsonDataRepository.loadStockData(ticker);

        if (data.isEmpty()) {
            log.error("Data file for {} is empty", ticker);
            throw new RuntimeException("Data file for " + ticker + " is empty");
        }

        log.info("Loaded {} records for {} (from {} to {})",
            data.size(), ticker, data.get(0).getDate(), data.get(data.size()-1).getDate());

        return data;
    }
}
