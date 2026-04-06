package com.backend.audit.model;

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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_record", indexes = {
        @Index(name = "idx_actor", columnList = "actor"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_resource", columnList = "resource"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
public class AuditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actor; // username or service name

    private String action; // CRUD verb

    private String resource; // resource name/id

    @Column(length = 2048)
    private String metadata; // JSON string for extra context

    @CreationTimestamp
    private LocalDateTime createdAt;
}
