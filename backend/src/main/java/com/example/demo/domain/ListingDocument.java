package com.example.demo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import lombok.Data;
import java.util.List;

@Data
@Document(indexName = "listings")
public class ListingDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Double)
    private Double pricePerNight;
    
    @Field(type = FieldType.Integer)
    private Integer maxGuests;
    
    @GeoPointField
    private GeoPoint location;
    
    @Field(type = FieldType.Keyword)
    private List<String> amenities;
}
