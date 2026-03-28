package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SearchRequest {
    private Double locationLat;
    private Double locationLon;
    private Double radiusKm;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private Double minPrice;
    private Double maxPrice;
    private Integer page = 0;
    private Integer size = 20;
}
