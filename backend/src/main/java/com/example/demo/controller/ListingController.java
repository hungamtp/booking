package com.example.demo.controller;

import com.example.demo.dto.ListingDetailResponse;
import com.example.demo.service.ListingBffService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingBffService listingBffService;

    @GetMapping("/{id}")
    public ListingDetailResponse getListing(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return listingBffService.getListingDetail(id, checkIn, checkOut);
    }
}
