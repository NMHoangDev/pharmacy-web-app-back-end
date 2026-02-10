package com.backend.branch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "branch_settings")
public class BranchSettings {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "branch_id", columnDefinition = "char(36)")
    private UUID branchId;

    @Column(name = "slot_duration_minutes")
    private int slotDurationMinutes;

    @Column(name = "buffer_before_minutes")
    private int bufferBeforeMinutes;

    @Column(name = "buffer_after_minutes")
    private int bufferAfterMinutes;

    @Column(name = "lead_time_minutes")
    private int leadTimeMinutes;

    @Column(name = "cutoff_time")
    private LocalTime cutoffTime;

    @Column(name = "max_bookings_per_pharmacist_per_day")
    private Integer maxBookingsPerPharmacistPerDay;

    @Column(name = "max_bookings_per_customer_per_week")
    private Integer maxBookingsPerCustomerPerWeek;

    @Column(name = "channels_json", columnDefinition = "json")
    private String channelsJson;

    @Column(name = "pricing_json", columnDefinition = "json")
    private String pricingJson;

    @Column(name = "pickup_enabled")
    private boolean pickupEnabled;

    @Column(name = "delivery_enabled")
    private boolean deliveryEnabled;

    @Column(name = "delivery_zones_json", columnDefinition = "json")
    private String deliveryZonesJson;

    @Column(name = "shipping_fee_rules_json", columnDefinition = "json")
    private String shippingFeeRulesJson;

    @Column(name = "default_warehouse_code", length = 64)
    private String defaultWarehouseCode;

    @Column(name = "allow_negative_stock")
    private boolean allowNegativeStock;

    @Column(name = "default_reorder_point")
    private int defaultReorderPoint;

    @Column(name = "enable_fefo")
    private boolean enableFefo;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public int getBufferBeforeMinutes() {
        return bufferBeforeMinutes;
    }

    public void setBufferBeforeMinutes(int bufferBeforeMinutes) {
        this.bufferBeforeMinutes = bufferBeforeMinutes;
    }

    public int getBufferAfterMinutes() {
        return bufferAfterMinutes;
    }

    public void setBufferAfterMinutes(int bufferAfterMinutes) {
        this.bufferAfterMinutes = bufferAfterMinutes;
    }

    public int getLeadTimeMinutes() {
        return leadTimeMinutes;
    }

    public void setLeadTimeMinutes(int leadTimeMinutes) {
        this.leadTimeMinutes = leadTimeMinutes;
    }

    public LocalTime getCutoffTime() {
        return cutoffTime;
    }

    public void setCutoffTime(LocalTime cutoffTime) {
        this.cutoffTime = cutoffTime;
    }

    public Integer getMaxBookingsPerPharmacistPerDay() {
        return maxBookingsPerPharmacistPerDay;
    }

    public void setMaxBookingsPerPharmacistPerDay(Integer maxBookingsPerPharmacistPerDay) {
        this.maxBookingsPerPharmacistPerDay = maxBookingsPerPharmacistPerDay;
    }

    public Integer getMaxBookingsPerCustomerPerWeek() {
        return maxBookingsPerCustomerPerWeek;
    }

    public void setMaxBookingsPerCustomerPerWeek(Integer maxBookingsPerCustomerPerWeek) {
        this.maxBookingsPerCustomerPerWeek = maxBookingsPerCustomerPerWeek;
    }

    public String getChannelsJson() {
        return channelsJson;
    }

    public void setChannelsJson(String channelsJson) {
        this.channelsJson = channelsJson;
    }

    public String getPricingJson() {
        return pricingJson;
    }

    public void setPricingJson(String pricingJson) {
        this.pricingJson = pricingJson;
    }

    public boolean isPickupEnabled() {
        return pickupEnabled;
    }

    public void setPickupEnabled(boolean pickupEnabled) {
        this.pickupEnabled = pickupEnabled;
    }

    public boolean isDeliveryEnabled() {
        return deliveryEnabled;
    }

    public void setDeliveryEnabled(boolean deliveryEnabled) {
        this.deliveryEnabled = deliveryEnabled;
    }

    public String getDeliveryZonesJson() {
        return deliveryZonesJson;
    }

    public void setDeliveryZonesJson(String deliveryZonesJson) {
        this.deliveryZonesJson = deliveryZonesJson;
    }

    public String getShippingFeeRulesJson() {
        return shippingFeeRulesJson;
    }

    public void setShippingFeeRulesJson(String shippingFeeRulesJson) {
        this.shippingFeeRulesJson = shippingFeeRulesJson;
    }

    public String getDefaultWarehouseCode() {
        return defaultWarehouseCode;
    }

    public void setDefaultWarehouseCode(String defaultWarehouseCode) {
        this.defaultWarehouseCode = defaultWarehouseCode;
    }

    public boolean isAllowNegativeStock() {
        return allowNegativeStock;
    }

    public void setAllowNegativeStock(boolean allowNegativeStock) {
        this.allowNegativeStock = allowNegativeStock;
    }

    public int getDefaultReorderPoint() {
        return defaultReorderPoint;
    }

    public void setDefaultReorderPoint(int defaultReorderPoint) {
        this.defaultReorderPoint = defaultReorderPoint;
    }

    public boolean isEnableFefo() {
        return enableFefo;
    }

    public void setEnableFefo(boolean enableFefo) {
        this.enableFefo = enableFefo;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
