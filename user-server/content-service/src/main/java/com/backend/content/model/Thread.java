package com.backend.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threads")
public class Thread {

    @Id
    @Column(name = "id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "context_json", columnDefinition = "json")
    private String contextJson;

    @Column(name = "asker_id", nullable = false, columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID askerId;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    @Column(name = "thread_status", nullable = false, length = 16)
    private ThreadStatus threadStatus = ThreadStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 16)
    private ModerationStatus moderationStatus = ModerationStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "answer_count", nullable = false)
    private int answerCount;

    @Column(name = "has_pharmacist_answer", nullable = false)
    private boolean hasPharmacistAnswer;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public UUID getAskerId() {
        return askerId;
    }

    public void setAskerId(UUID askerId) {
        this.askerId = askerId;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public ThreadStatus getThreadStatus() {
        return threadStatus;
    }

    public void setThreadStatus(ThreadStatus threadStatus) {
        this.threadStatus = threadStatus;
    }

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(int answerCount) {
        this.answerCount = answerCount;
    }

    public boolean isHasPharmacistAnswer() {
        return hasPharmacistAnswer;
    }

    public void setHasPharmacistAnswer(boolean hasPharmacistAnswer) {
        this.hasPharmacistAnswer = hasPharmacistAnswer;
    }
}
