package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.playmotech.api.core.constants.DiscoverCategory;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "discover")
public class Discover {
    @Id
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DiscoverCategory type;

    private String specialty;
    private String location;

    // Geolocation for maps & near me
    private Double latitude;
    private Double longitude;

    private double rating;
    private Integer reviewCount;

    private String phone;
    private String website;
    private String directionsLink;

    @ElementCollection
    @CollectionTable(name = "discover_images", joinColumns = @JoinColumn(name = "discover_id"))
    @Column(name = "image_url")
    private List<String> imageGallery;

    private boolean affiliated;
    private boolean scraped; // true if data came from web scraping
    private boolean featured; // highlight in listings
    private boolean active; // toggle visibility

    // Store as JSON string for flexible structure (e.g., {"mon":"9am-6pm","tue":"9am-6pm",...})
    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHoursJson;

    // Search/filter enhancements
    @ElementCollection
    @CollectionTable(name = "discover_tags", joinColumns = @JoinColumn(name = "discover_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ElementCollection
    @CollectionTable(name = "discover_services", joinColumns = @JoinColumn(name = "discover_id"))
    @Column(name = "service")
    private List<String> servicesOffered;

    // Timestamps
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
