package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InvestingApiResponse {
    private List<HistoricalDataPoint> data;

    @Data
    public static class HistoricalDataPoint {
        @JsonProperty("rowDateTimestamp")
        private String rowDateTimestamp;  // "2025-11-14T00:00:00Z"

        @JsonProperty("last_closeRaw")
        private Double lastCloseRaw;

        @JsonProperty("last_openRaw")
        private Double lastOpenRaw;

        @JsonProperty("last_maxRaw")
        private Double lastMaxRaw;

        @JsonProperty("last_minRaw")
        private Double lastMinRaw;

        @JsonProperty("volumeRaw")
        private Long volumeRaw;
    }
}
