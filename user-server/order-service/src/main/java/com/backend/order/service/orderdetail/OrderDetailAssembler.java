package com.backend.order.service.orderdetail;

import com.backend.order.api.dto.OrderDetailItemResponse;
import com.backend.order.api.dto.OrderDetailResponse;
import com.backend.order.api.dto.OrderDetailShippingAddressResponse;
import com.backend.order.api.dto.OrderStatusHistoryResponse;
import com.backend.order.api.dto.OrderTrackingResponse;
import com.backend.order.model.OrderEntity;
import com.backend.order.model.OrderItem;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderDetailAssembler {

    public OrderDetailResponse toResponse(
            OrderEntity order,
            List<OrderItem> items,
            Map<UUID, ProductSnapshot> productById,
            OrderDetailShippingAddressResponse shippingAddress) {
        List<OrderDetailItemResponse> detailItems = items.stream()
                .map(item -> toItem(item, productById.get(item.getId().getProductId())))
                .toList();

        Instant changedAt = order.getUpdatedAt() == null ? order.getCreatedAt() : order.getUpdatedAt();
        List<OrderStatusHistoryResponse> statusHistory = List.of(new OrderStatusHistoryResponse(
                order.getStatus().name(),
                changedAt,
                "Current order status"));

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUserId(),
                order.getStatus().name(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getNote(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                shippingAddress,
                detailItems,
                new OrderTrackingResponse(order.getStatus().name(), statusHistory));
    }

    private OrderDetailItemResponse toItem(OrderItem item, ProductSnapshot product) {
        String productName = firstNonBlank(item.getProductName(), product == null ? null : product.name());
        double lineTotal = item.getUnitPrice() * item.getQuantity();
        return new OrderDetailItemResponse(
                item.getId().getProductId(),
                productName,
                firstNonBlank(item.getImageUrl(), product == null ? null : product.imageUrl()),
                firstNonBlank(item.getSku(), product == null ? null : product.sku()),
                firstNonBlank(item.getUnit(), product == null ? null : product.unit()),
                item.getUnitPrice(),
                item.getQuantity(),
                lineTotal,
                firstNonBlank(item.getCategory(), product == null ? null : product.category()),
                firstNonBlank(item.getType(), product == null ? null : product.type()),
                firstNonBlank(item.getShortDescription(), product == null ? null : product.shortDescription()));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
