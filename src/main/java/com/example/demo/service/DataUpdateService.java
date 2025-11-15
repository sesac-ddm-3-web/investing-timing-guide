package com.example.demo.service;

import com.example.demo.dto.InvestingApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataUpdateService {

    private static final String DATA_DIR = "src/main/resources/history";
    private static final String API_URL = "https://api.investing.com/api/financialdata/historical/%s?start-date=%s&end-date=%s&time-frame=Daily&add-missing-rows=false";

    // Ticker to Investing.com ID mapping
    private static final Map<String, String> TICKER_ID_MAP = Map.of(
        "QQQ", "651",
        "VOO", "38165",
        "SOXX", "45481"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 매일 오전 9시에 실행 (한국 시간 기준)
     * 미국 장 마감 후 데이터가 업데이트되므로, 한국 시간 오전에 실행하면 전날 데이터를 가져올 수 있음
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void updateAllTickersData() {
        log.info("Starting scheduled data update for all tickers");

        for (String ticker : TICKER_ID_MAP.keySet()) {
            try {
                updateTickerData(ticker);
            } catch (Exception e) {
                log.error("Failed to update data for ticker: {}", ticker, e);
            }
        }

        log.info("Completed scheduled data update");
    }

    /**
     * 특정 ticker의 데이터를 업데이트
     */
    public void updateTickerData(String ticker) throws IOException {
        String investingId = TICKER_ID_MAP.get(ticker);
        if (investingId == null) {
            throw new IllegalArgumentException("Unknown ticker: " + ticker);
        }

        String csvFilePath = DATA_DIR + "/" + ticker + ".csv";
        Path path = Paths.get(csvFilePath);

        // 기존 파일에서 마지막 날짜 확인
        LocalDate lastDate = getLastDateFromCsv(path);
        LocalDate today = LocalDate.now();

        // 이미 최신 데이터가 있으면 스킵
        if (lastDate != null && !lastDate.isBefore(today.minusDays(1))) {
            log.info("Data for {} is already up to date. Last date: {}", ticker, lastDate);
            return;
        }

        // API에서 데이터 가져오기
        LocalDate startDate = lastDate != null ? lastDate.plusDays(1) : LocalDate.of(1975, 1, 1);
        String url = String.format(API_URL, investingId, startDate, today);

        log.info("Fetching data for {} from {} to {}", ticker, startDate, today);

        InvestingApiResponse response = restTemplate.getForObject(url, InvestingApiResponse.class);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            log.info("No new data available for {}", ticker);
            return;
        }

        // CSV 파일에 추가
        appendToCsv(path, response.getData());

        log.info("Successfully updated {} records for {}", response.getData().size(), ticker);
    }

    /**
     * CSV 파일에서 마지막 날짜 가져오기
     */
    private LocalDate getLastDateFromCsv(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        List<String> lines = Files.readAllLines(path);
        if (lines.size() <= 1) { // 헤더만 있거나 빈 파일
            return null;
        }

        // 마지막 줄에서 날짜 추출
        String lastLine = lines.get(lines.size() - 1);
        String[] parts = lastLine.split(",");
        if (parts.length > 0) {
            return LocalDate.parse(parts[0]);
        }

        return null;
    }

    /**
     * CSV 파일에 새 데이터 추가
     */
    private void appendToCsv(Path path, List<InvestingApiResponse.HistoricalDataPoint> dataPoints) throws IOException {
        // 날짜 순으로 정렬 (오래된 것부터)
        List<InvestingApiResponse.HistoricalDataPoint> sortedData = dataPoints.stream()
            .sorted(Comparator.comparing(InvestingApiResponse.HistoricalDataPoint::getRowDateTimestamp))
            .collect(Collectors.toList());

        // 파일이 없으면 헤더 추가
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.write(path, Collections.singletonList("Date,Open,High,Low,Close,Volume"),
                StandardOpenOption.CREATE);
        }

        // 데이터 추가
        List<String> lines = new ArrayList<>();
        for (InvestingApiResponse.HistoricalDataPoint dataPoint : sortedData) {
            // ISO 타임스탬프에서 날짜만 추출 (2025-11-14T00:00:00Z -> 2025-11-14)
            String date = dataPoint.getRowDateTimestamp().substring(0, 10);

            String line = String.format("%s,%.2f,%.2f,%.2f,%.2f,%d",
                date,
                dataPoint.getLastOpenRaw(),
                dataPoint.getLastMaxRaw(),
                dataPoint.getLastMinRaw(),
                dataPoint.getLastCloseRaw(),
                dataPoint.getVolumeRaw()
            );
            lines.add(line);
        }

        Files.write(path, lines, StandardOpenOption.APPEND);
    }

    /**
     * 수동으로 특정 ticker 업데이트 (테스트용)
     */
    public String manualUpdate(String ticker) {
        try {
            updateTickerData(ticker);
            return "Successfully updated " + ticker;
        } catch (Exception e) {
            log.error("Error updating ticker: {}", ticker, e);
            return "Failed to update " + ticker + ": " + e.getMessage();
        }
    }
}
