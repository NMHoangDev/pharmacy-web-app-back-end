package com.backend.catalog.repo;

import java.math.BigDecimal;
import java.util.UUID;

public interface DrugWithBranchView {
    UUID getId();

    String getSku();

    String getName();

    String getSlug();

    UUID getCategoryId();

    BigDecimal getCostPrice();

    BigDecimal getBaseSalePrice();

    String getGlobalStatus();

    boolean isPrescriptionRequired();

    String getDescription();

    String getImageUrl();

    String getAttributes();

    BigDecimal getPriceOverride();

    String getBranchStatus();

    String getNote();
}
