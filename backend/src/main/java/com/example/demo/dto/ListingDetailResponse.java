package com.example.demo.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

import java.io.Serializable;

@Data
public class ListingDetailResponse implements Serializable {
    private String id;
    private Double pricePerNight;
    private Integer maxGuests;
    private List<String> amenities;
    private List<String> photos; // CDN URLs
    
    private Map<LocalDate, Boolean> availabilityCalendar; // date -> isBlocked
    private ReviewSummary reviews;

    @Data
    public static class ReviewSummary implements Serializable {
        private Double averageRating;
        private List<ReviewItem> recentReviews;
    }

    @Data
    public static class ReviewItem implements Serializable {
        private String author;
        private String comment;
        private Integer rating;
    }
}
