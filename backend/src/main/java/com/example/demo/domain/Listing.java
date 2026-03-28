package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "listings")
@Data
@NoArgsConstructor
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    
    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;
    
    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;
    
    @Column(name = "location_lat", nullable = false)
    private BigDecimal locationLat;
    
    @Column(name = "location_lon", nullable = false)
    private BigDecimal locationLon;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> amenities;
    
    @Column(nullable = false)
    private String status = "ACTIVE";
}
