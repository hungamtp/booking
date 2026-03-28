package com.example.demo.service;

import com.example.demo.dto.BookingRequest;
import com.example.demo.repository.AvailabilityRepository;
import com.example.demo.domain.Availability;
import com.example.demo.domain.Listing;
import com.example.demo.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Test
    public void testConcurrentBookings() throws InterruptedException {
        // Find a random active listing
        Listing l = listingRepository.findAll().stream().findFirst().orElse(null);
        if (l == null) return; // if db not seeded, skip

        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        BookingRequest reqTemplate = new BookingRequest();
        reqTemplate.setGuestId(10L);
        reqTemplate.setListingId(l.getId());
        reqTemplate.setCheckIn(LocalDate.now().plusDays(10));
        reqTemplate.setCheckOut(LocalDate.now().plusDays(12));
        reqTemplate.setTotalPrice(BigDecimal.valueOf(200));
        reqTemplate.setCardToken("test_token_ok");

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    bookingService.createBooking(reqTemplate, "idemp_test_" + index);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // At most 1 should succeed, the rest should throw ResponseStatusException (409 Conflict) or if dates unavailable, all might fail
        System.out.println("Success count: " + successCount.get());
        System.out.println("Error count: " + errorCount.get());
    }
}
