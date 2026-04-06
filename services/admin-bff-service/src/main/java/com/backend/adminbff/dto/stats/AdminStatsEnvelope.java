package com.backend.adminbff.dto.stats;

import java.time.Instant;

public record AdminStatsEnvelope<T>(T metrics, Instant generatedAt, String range) {
}
