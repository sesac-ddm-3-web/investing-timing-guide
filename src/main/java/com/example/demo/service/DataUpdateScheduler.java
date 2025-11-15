package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 데이터 업데이트 스케줄러 (비활성화됨)
 *
 * Yahoo Finance API 대신 로컬 CSV 데이터를 사용하므로 자동 업데이트는 비활성화되었습니다.
 * 데이터를 업데이트하려면:
 * 1. history/ 폴더의 CSV 파일을 업데이트
 * 2. python3 convert_csv_to_json.py 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataUpdateScheduler {

    private static final List<String> TICKERS = Arrays.asList("QQQ", "VOO", "SOXX");

    /**
     * Scheduled data update is DISABLED
     * Using local CSV data instead of Yahoo Finance API
     *
     * To update data:
     * 1. Update CSV files in history/ folder
     * 2. Run: python3 convert_csv_to_json.py
     */
    // @Scheduled(cron = "0 0 21 * * ?")
    // public void updateStockData() {
    //     log.info("Scheduled update is disabled - using local CSV data");
    // }
}
