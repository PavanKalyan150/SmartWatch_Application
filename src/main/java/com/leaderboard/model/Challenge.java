package com.leaderboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "challenges")
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Integer requiredSteps;

    @Column(nullable = false)
    private Integer pointsReward;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "challenge_features", joinColumns = @JoinColumn(name = "challenge_id"))
    @Column(name = "feature")
    private Set<String> requiredFeatures = new HashSet<>();

    // Geospatial & City Scope
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private String city;

    @Column(nullable = false)
    private Boolean isGlobal = true;

    // Status: tracks if ranking batch job has processed this challenge
    @Column(nullable = false)
    private Boolean isProcessed = false;

    public Challenge() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRequiredSteps() {
        return requiredSteps;
    }

    public void setRequiredSteps(Integer requiredSteps) {
        this.requiredSteps = requiredSteps;
    }

    public Integer getPointsReward() {
        return pointsReward;
    }

    public void setPointsReward(Integer pointsReward) {
        this.pointsReward = pointsReward;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Set<String> getRequiredFeatures() {
        return requiredFeatures;
    }

    public void setRequiredFeatures(Set<String> requiredFeatures) {
        this.requiredFeatures = requiredFeatures;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(Boolean global) {
        isGlobal = global;
    }

    public Boolean getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(Boolean processed) {
        isProcessed = processed;
    }
}
