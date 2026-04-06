package com.backend.content.repo;

import java.util.UUID;

public interface ThreadAnswerCount {
    UUID getThreadId();

    long getTotal();
}
