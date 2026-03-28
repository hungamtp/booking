package com.example.demo.dto;

import lombok.Data;
import java.util.List;

import java.io.Serializable;

@Data
public class SearchResponse implements Serializable {
    private List<ListingSummary> results;
    private long totalElements;

    @Data
    public static class ListingSummary implements Serializable {
        private String id;
        private Double pricePerNight;
        private Integer maxGuests;
        private List<String> amenities;
    }
}
