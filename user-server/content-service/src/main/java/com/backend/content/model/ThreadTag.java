package com.backend.content.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "thread_tags")
public class ThreadTag {

    @EmbeddedId
    private ThreadTagId id;

    public ThreadTag() {
    }

    public ThreadTag(ThreadTagId id) {
        this.id = id;
    }

    public ThreadTagId getId() {
        return id;
    }

    public void setId(ThreadTagId id) {
        this.id = id;
    }
}
