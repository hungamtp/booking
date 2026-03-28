package com.example.demo.service;

import com.example.demo.domain.Listing;
import com.example.demo.dto.ListingSnapshot;
import com.example.demo.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingSnapshotService {
    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "listing_snapshot", key = "#id")
    public ListingSnapshot getListingSnapshot(Long id) {
        log.info("Fetching listing snapshot from DB for id: {}", id);
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        ListingSnapshot snapshot = new ListingSnapshot();
        snapshot.setId(listing.getId());
        snapshot.setPricePerNight(listing.getPricePerNight().doubleValue());
        snapshot.setMaxGuests(listing.getMaxGuests());
        snapshot.setAmenities(listing.getAmenities() != null ? listing.getAmenities() : new ArrayList<>());
        snapshot.setPhotos(List.of(
            "https://cdn.example.com/photos/" + id + "/1.jpg?sig=abc",
            "https://cdn.example.com/photos/" + id + "/2.jpg?sig=def"
        ));
        return snapshot;
    }
}
