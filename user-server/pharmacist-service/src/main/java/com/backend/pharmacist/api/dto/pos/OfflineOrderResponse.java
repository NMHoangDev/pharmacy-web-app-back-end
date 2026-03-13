package com.backend.pharmacist.api.dto.pos;

import com.backend.pharmacist.model.OfflineOrderStatus;
import com.backend.pharmacist.model.OfflinePaymentMethod;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfflineOrderResponse(
        UUID id,
        String orderCode,
        String orderType,
        UUID branchId,
        UUID pharmacistId,
        String customerName,
        String customerPhone,
        String consultationId,
        String note,
        Long subtotal,
        Long discount,
        Long taxFee,
        Long totalAmount,
        OfflineOrderStatus status,
        OfflinePaymentMethod paymentMethod,
        Long amountReceived,
        Long changeAmount,
        String transferReference,
        String paymentProofUrl,
        UUID inventoryReservationId,
        Instant paidAt,
        Instant refundedAt,
        Instant createdAt,
        Instant updatedAt,
        List<OfflineOrderItemResponse> items,
        List<OfflinePaymentResponse> payments) {
}
