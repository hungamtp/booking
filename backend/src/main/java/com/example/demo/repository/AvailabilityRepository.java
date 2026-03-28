package com.example.demo.repository;

import com.example.demo.domain.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    @Query(value = "SELECT a.listing_id FROM availability a " +
           "WHERE a.listing_id IN :listingIds " +
           "AND a.date >= :checkIn " +
           "AND a.date < :checkOut " +
           "AND a.is_blocked = true", nativeQuery = true)
    List<Long> findBlockedListingIds(@Param("listingIds") List<Long> listingIds,
                                     @Param("checkIn") LocalDate checkIn, 
                                     @Param("checkOut") LocalDate checkOut);

    List<Availability> findByListingIdAndDateBetween(Long listingId, LocalDate startDate, LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Availability a WHERE a.listing.id = :listingId AND a.date >= :checkIn AND a.date < :checkOut")
    List<Availability> findAvailabilitiesForLocking(@Param("listingId") Long listingId, 
                                                    @Param("checkIn") LocalDate checkIn, 
                                                    @Param("checkOut") LocalDate checkOut);
}
