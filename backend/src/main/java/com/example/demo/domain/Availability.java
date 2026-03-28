package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "availability", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"listing_id", "date"})
})
@Data
@NoArgsConstructor
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;
}
