package com.backend.settings.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "app_setting", indexes = {
        @Index(name = "idx_scope_key", columnList = "scope, settingKey", unique = true)
})
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String scope; // global, notification, payment, etc.

    @Column(name = "settingKey")
    private String key;

    @Column(length = 2048)
    private String value;

    @Column(length = 1024)
    private String description;

    private boolean secure; // mask in UI/logs

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
