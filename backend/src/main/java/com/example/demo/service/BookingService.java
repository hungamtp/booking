package com.example.demo.service;

import com.example.demo.domain.Availability;
import com.example.demo.domain.Booking;
import com.example.demo.domain.Listing;
import com.example.demo.domain.Payment;
import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.BookingResponse;
import com.example.demo.repository.AvailabilityRepository;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public BookingResponse createBooking(BookingRequest req, String idempotencyKey) {
        String cacheKey = "idemp:booking:" + idempotencyKey;
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return objectMapper.readValue(cachedJson, BookingResponse.class);
            }
        } catch (Exception e) {
            log.warn("Error reading from redis for idempotency", e);
        }

        // 1. Pessimistic lock on availability rows
        List<Availability> nights = availabilityRepository.findAvailabilitiesForLocking(req.getListingId(), req.getCheckIn(), req.getCheckOut());
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut());
        if (nights.size() != days || nights.stream().anyMatch(Availability::getIsBlocked)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dates are not available");
        }

        // 2. Mark as blocked
        nights.forEach(a -> a.setIsBlocked(true));
        availabilityRepository.saveAll(nights);

        // 3. Create pending booking
        Booking booking = new Booking();
        booking.setGuestId(req.getGuestId());
        Listing listing = new Listing();
        listing.setId(req.getListingId());
        booking.setListing(listing);
        booking.setCheckIn(req.getCheckIn());
        booking.setCheckOut(req.getCheckOut());
        booking.setStatus("PENDING");
        booking.setIdempotencyKey(idempotencyKey);
        booking.setTotalPrice(req.getTotalPrice());
        booking = bookingRepository.save(booking);

        // 4. Payment
        boolean paymentSuccess = paymentService.authorize(booking.getTotalPrice(), req.getCardToken());
        if (!paymentSuccess) {
            booking.setStatus("CANCELLED");
            nights.forEach(a -> a.setIsBlocked(false));
            availabilityRepository.saveAll(nights);
            bookingRepository.save(booking);
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment failed");
        }

        // 5. Confirm and save payment
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setGatewayRef("mock_ref_" + System.currentTimeMillis());
        payment.setStatus("CAPTURED");
        payment.setCapturedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        booking.setStatus("CONFIRMED");
        booking = bookingRepository.save(booking);

        BookingResponse response = new BookingResponse(booking.getId(), booking.getStatus());
        
        // Save to Redis
        try {
            String jsonResp = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, jsonResp, Duration.ofHours(24));
        } catch (Exception e) {
            log.error("Could not save to redis", e);
        }

        // 6. Kafka Event
        try {
            String bookingJson = objectMapper.writeValueAsString(response);
            kafkaTemplate.send("booking.confirmed", String.valueOf(booking.getId()), bookingJson);
        } catch (Exception e) {
            log.error("Failed to send Kafka event", e);
        }

        return response;
    }
}
