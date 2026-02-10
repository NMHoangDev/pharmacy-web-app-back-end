package com.backend.branch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BranchStaffId implements Serializable {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "branch_id", columnDefinition = "char(36)")
    private UUID branchId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", columnDefinition = "char(36)")
    private UUID userId;

    public BranchStaffId() {
    }

    public BranchStaffId(UUID branchId, UUID userId) {
        this.branchId = branchId;
        this.userId = userId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BranchStaffId that = (BranchStaffId) o;
        return Objects.equals(branchId, that.branchId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchId, userId);
    }
}
