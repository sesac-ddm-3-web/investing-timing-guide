package com.example.demo.repository;

import com.example.demo.model.StockData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class JsonDataRepository {

    private static final String DATA_DIR = "src/main/resources/data";
    private final ObjectMapper objectMapper;

    public JsonDataRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Create data directory if it doesn't exist
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void saveStockData(String ticker, List<StockData> stockDataList) {
        try {
            File file = new File(DATA_DIR + "/" + ticker + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, stockDataList);
            log.info("Saved {} records for {}", stockDataList.size(), ticker);
        } catch (IOException e) {
            log.error("Error saving stock data for {}", ticker, e);
            throw new RuntimeException("Failed to save stock data", e);
        }
    }

    public List<StockData> loadStockData(String ticker) {
        try {
            File file = new File(DATA_DIR + "/" + ticker + ".json");
            if (!file.exists()) {
                log.warn("No data file found for {}", ticker);
                return new ArrayList<>();
            }

            List<StockData> data = objectMapper.readValue(file, new TypeReference<List<StockData>>() {});
            log.info("Loaded {} records for {}", data.size(), ticker);
            return data;
        } catch (IOException e) {
            log.error("Error loading stock data for {}", ticker, e);
            return new ArrayList<>();
        }
    }

    public boolean hasData(String ticker) {
        File file = new File(DATA_DIR + "/" + ticker + ".json");
        return file.exists();
    }

    public void saveAnalysisCache(String ticker, Map<String, Object> analysis) {
        try {
            File file = new File(DATA_DIR + "/" + ticker + "_analysis.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, analysis);
            log.info("Saved analysis cache for {}", ticker);
        } catch (IOException e) {
            log.error("Error saving analysis cache for {}", ticker, e);
        }
    }

    /**
     * Append recent data to existing stock data, avoiding duplicates
     */
    public void appendStockData(String ticker, List<StockData> newData) {
        try {
            List<StockData> existingData = loadStockData(ticker);

            if (existingData.isEmpty()) {
                // No existing data, just save the new data
                saveStockData(ticker, newData);
                return;
            }

            // Merge new data with existing, avoiding duplicates
            List<StockData> mergedData = new ArrayList<>(existingData);

            for (StockData newRecord : newData) {
                boolean exists = existingData.stream()
                    .anyMatch(existing -> existing.getDate().equals(newRecord.getDate()));

                if (!exists) {
                    mergedData.add(newRecord);
                }
            }

            // Sort by date
            mergedData.sort((a, b) -> a.getDate().compareTo(b.getDate()));

            // Save merged data
            saveStockData(ticker, mergedData);
            log.info("Appended {} new records to {} (total: {})",
                mergedData.size() - existingData.size(), ticker, mergedData.size());

        } catch (Exception e) {
            log.error("Error appending stock data for {}", ticker, e);
            throw new RuntimeException("Failed to append stock data", e);
        }
    }
}
