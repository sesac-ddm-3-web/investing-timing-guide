package com.example.demo.controller;

import com.example.demo.service.DataUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자용 데이터 업데이트 API
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataUpdateService dataUpdateService;

    /**
     * 특정 ticker의 데이터를 수동으로 업데이트
     * 예: GET /api/admin/update/QQQ
     */
    @GetMapping("/update/{ticker}")
    public ResponseEntity<String> updateTicker(@PathVariable String ticker) {
        String result = dataUpdateService.manualUpdate(ticker);
        return ResponseEntity.ok(result);
    }

    /**
     * 모든 ticker의 데이터를 수동으로 업데이트
     * 예: GET /api/admin/update-all
     */
    @GetMapping("/update-all")
    public ResponseEntity<String> updateAllTickers() {
        dataUpdateService.updateAllTickersData();
        return ResponseEntity.ok("Update process initiated for all tickers");
    }
}
