package com.example.demo.service;

import com.example.demo.domain.Listing;
import com.example.demo.domain.ListingDocument;
import com.example.demo.repository.ListingRepository;
import com.example.demo.repository.ListingSearchRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingIndexerService {
    private final ListingRepository listingRepository;
    private final ListingSearchRepository listingSearchRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void indexAllListings() {
        try {
            log.info("Starting initial Elasticsearch indexing...");
            long count = listingSearchRepository.count();
            if (count > 0) {
                log.info("Index 'listings' already contains {} documents. Skipping initialization.", count);
                return;
            }

            List<Listing> allListings = listingRepository.findAll();
            List<ListingDocument> documents = allListings.stream().map(listing -> {
                ListingDocument doc = new ListingDocument();
                doc.setId(listing.getId().toString());
                doc.setPricePerNight(listing.getPricePerNight().doubleValue());
                doc.setMaxGuests(listing.getMaxGuests());
                doc.setLocation(new GeoPoint(listing.getLocationLat().doubleValue(), listing.getLocationLon().doubleValue()));
                doc.setAmenities(listing.getAmenities() != null ? listing.getAmenities() : new ArrayList<>());
                return doc;
            }).collect(Collectors.toList());

            listingSearchRepository.saveAll(documents);
            log.info("Indexed {} listings into Elasticsearch.", documents.size());
        } catch (Exception ex) {
            log.warn("Could not index initial listings to Elasticsearch: {}", ex.getMessage());
        }
    }
}
