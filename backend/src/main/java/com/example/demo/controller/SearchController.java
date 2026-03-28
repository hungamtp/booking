package com.example.demo.controller;

import com.example.demo.dto.SearchRequest;
import com.example.demo.dto.SearchResponse;
import com.example.demo.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    @PostMapping
    public SearchResponse search(@RequestBody SearchRequest request) {
        return searchService.searchListings(request);
    }
}
