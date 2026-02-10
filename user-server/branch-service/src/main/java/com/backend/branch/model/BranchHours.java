package com.backend.branch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "branch_hours")
public class BranchHours {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "branch_id", columnDefinition = "char(36)")
    private UUID branchId;

    @Column(name = "weekly_hours_json", columnDefinition = "json")
    private String weeklyHoursJson;

    @Column(name = "lunch_break_json", columnDefinition = "json")
    private String lunchBreakJson;

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getWeeklyHoursJson() {
        return weeklyHoursJson;
    }

    public void setWeeklyHoursJson(String weeklyHoursJson) {
        this.weeklyHoursJson = weeklyHoursJson;
    }

    public String getLunchBreakJson() {
        return lunchBreakJson;
    }

    public void setLunchBreakJson(String lunchBreakJson) {
        this.lunchBreakJson = lunchBreakJson;
    }
}
