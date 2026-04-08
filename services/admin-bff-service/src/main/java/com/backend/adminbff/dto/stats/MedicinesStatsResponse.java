package com.backend.adminbff.dto.stats;

public record MedicinesStatsResponse(
        long totalMedicines,
        long activeMedicines,
        long lowStockMedicines,
        long outOfStockMedicines,
        long hiddenOrDiscontinuedMedicines) {
}
