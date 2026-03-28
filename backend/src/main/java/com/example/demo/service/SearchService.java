package com.example.demo.service;

import com.example.demo.domain.ListingDocument;
import com.example.demo.dto.SearchRequest;
import com.example.demo.dto.SearchResponse;
import com.example.demo.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final AvailabilityRepository availabilityRepository;

    @Cacheable(value = "search_results", key = "#request.hashCode()", unless = "#result.results.isEmpty()")
    public SearchResponse searchListings(SearchRequest request) {
        log.info("Executing search for request: {}", request);
        
        Criteria criteria = null;
        
        if (request.getLocationLat() != null && request.getLocationLon() != null && request.getRadiusKm() != null) {
            criteria = new Criteria("location").within(new GeoPoint(request.getLocationLat(), request.getLocationLon()), request.getRadiusKm() + "km");
        }
        
        if (request.getGuests() != null) {
            Criteria guestCriteria = new Criteria("maxGuests").greaterThanEqual(request.getGuests());
            criteria = (criteria == null) ? guestCriteria : criteria.and(guestCriteria);
        }
        
        if (request.getMinPrice() != null) {
            Criteria minPriceCriteria = new Criteria("pricePerNight").greaterThanEqual(request.getMinPrice());
            criteria = (criteria == null) ? minPriceCriteria : criteria.and(minPriceCriteria);
        }
        if (request.getMaxPrice() != null) {
            Criteria maxPriceCriteria = new Criteria("pricePerNight").lessThanEqual(request.getMaxPrice());
            criteria = (criteria == null) ? maxPriceCriteria : criteria.and(maxPriceCriteria);
        }

        if (criteria == null) {
            // Match all query fallback if no criteria provided
            criteria = new Criteria("id").exists(); 
        }

        Query searchQuery = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(request.getPage(), request.getSize()));

        SearchHits<ListingDocument> searchHits = elasticsearchOperations.search(searchQuery, ListingDocument.class);
        
        List<ListingDocument> esResults = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        if (esResults.isEmpty()) {
            SearchResponse response = new SearchResponse();
            response.setResults(List.of());
            response.setTotalElements(0);
            return response;
        }

        List<Long> listingIds = esResults.stream()
                .map(doc -> Long.parseLong(doc.getId()))
                .collect(Collectors.toList());

        List<Long> blockedIds = List.of();
        if (request.getCheckIn() != null && request.getCheckOut() != null) {
            blockedIds = availabilityRepository.findBlockedListingIds(listingIds, request.getCheckIn(), request.getCheckOut());
        }

        List<Long> finalBlockedIds = blockedIds;
        List<SearchResponse.ListingSummary> availableResults = esResults.stream()
                .filter(doc -> !finalBlockedIds.contains(Long.parseLong(doc.getId())))
                .map(doc -> {
                    SearchResponse.ListingSummary summary = new SearchResponse.ListingSummary();
                    summary.setId(doc.getId());
                    summary.setPricePerNight(doc.getPricePerNight());
                    summary.setMaxGuests(doc.getMaxGuests());
                    summary.setAmenities(doc.getAmenities());
                    return summary;
                })
                .collect(Collectors.toList());

        SearchResponse response = new SearchResponse();
        response.setResults(availableResults);
        response.setTotalElements(availableResults.size());

        return response;
    }
}
