package com.example.demo.repository;

import com.example.demo.domain.ListingDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ListingSearchRepository extends ElasticsearchRepository<ListingDocument, String> {
}
