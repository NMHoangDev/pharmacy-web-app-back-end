package com.backend.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pharmacist_profiles")
@Getter
@Setter
public class PharmacistProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String licenseNo;

    @ElementCollection
    @CollectionTable(name = "pharmacist_specialties", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "specialty")
    private List<String> specialtyTags;

    private int yearsExp;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ElementCollection
    @CollectionTable(name = "pharmacist_languages", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "language")
    private List<String> languages;

    @Enumerated(EnumType.STRING)
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    public enum OnlineStatus {
        ONLINE, OFFLINE, BUSY
    }
}
