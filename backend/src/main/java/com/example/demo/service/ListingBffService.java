package com.example.demo.service;

import com.example.demo.domain.Availability;
import com.example.demo.dto.ListingDetailResponse;
import com.example.demo.dto.ListingSnapshot;
import com.example.demo.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingBffService {

    private final ListingSnapshotService listingSnapshotService;
    private final AvailabilityRepository availabilityRepository;

    public ListingDetailResponse getListingDetail(Long id, LocalDate checkIn, LocalDate checkOut) {
        CompletableFuture<ListingSnapshot> snapshotFuture = CompletableFuture.supplyAsync(() -> listingSnapshotService.getListingSnapshot(id));
        CompletableFuture<Map<LocalDate, Boolean>> availabilityFuture = CompletableFuture.supplyAsync(() -> fetchAvailabilityCalendar(id, checkIn, checkOut));
        CompletableFuture<ListingDetailResponse.ReviewSummary> reviewsFuture = CompletableFuture.supplyAsync(() -> fetchMockReviews(id));

        CompletableFuture.allOf(snapshotFuture, availabilityFuture, reviewsFuture).join();

        ListingSnapshot snapshot = snapshotFuture.join();
        
        ListingDetailResponse response = new ListingDetailResponse();
        response.setId(snapshot.getId().toString());
        response.setPricePerNight(snapshot.getPricePerNight());
        response.setMaxGuests(snapshot.getMaxGuests());
        response.setAmenities(snapshot.getAmenities());
        response.setPhotos(snapshot.getPhotos());

        response.setAvailabilityCalendar(availabilityFuture.join());
        response.setReviews(reviewsFuture.join());

        return response;
    }

    private Map<LocalDate, Boolean> fetchAvailabilityCalendar(Long id, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now();
        if (end == null) end = start.plusDays(90);

        List<Availability> availabilities = availabilityRepository.findByListingIdAndDateBetween(id, start, end);
        return availabilities.stream()
                .collect(Collectors.toMap(Availability::getDate, Availability::getIsBlocked));
    }

    private ListingDetailResponse.ReviewSummary fetchMockReviews(Long id) {
        ListingDetailResponse.ReviewSummary summary = new ListingDetailResponse.ReviewSummary();
        summary.setAverageRating(4.8);
        
        ListingDetailResponse.ReviewItem r1 = new ListingDetailResponse.ReviewItem();
        r1.setAuthor("Alice");
        r1.setRating(5);
        r1.setComment("Great place to stay!");
        
        summary.setRecentReviews(List.of(r1));
        return summary;
    }
}
