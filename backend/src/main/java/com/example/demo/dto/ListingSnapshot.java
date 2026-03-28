package com.example.demo.dto;

import lombok.Data;
import java.util.List;

import java.io.Serializable;

@Data
public class ListingSnapshot implements Serializable {
    private Long id;
    private Double pricePerNight;
    private Integer maxGuests;
    private List<String> amenities;
    private List<String> photos;
}
