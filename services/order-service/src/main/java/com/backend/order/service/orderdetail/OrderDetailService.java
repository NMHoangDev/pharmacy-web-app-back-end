package com.backend.order.service.orderdetail;

import com.backend.order.api.dto.OrderDetailResponse;
import com.backend.order.api.dto.OrderDetailShippingAddressResponse;
import com.backend.order.exception.ExternalServiceException;
import com.backend.order.exception.OrderAccessDeniedException;
import com.backend.order.exception.OrderNotFoundException;
import com.backend.order.model.OrderEntity;
import com.backend.order.model.OrderItem;
import com.backend.order.model.OrderShippingAddress;
import com.backend.order.repo.OrderItemRepository;
import com.backend.order.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderDetailService {

    private static final Logger log = LoggerFactory.getLogger(OrderDetailService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CatalogClient catalogClient;
    private final UserAddressClient userAddressClient;
    private final OrderDetailAssembler orderDetailAssembler;

    public OrderDetailService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CatalogClient catalogClient,
            UserAddressClient userAddressClient,
            OrderDetailAssembler orderDetailAssembler) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.catalogClient = catalogClient;
        this.userAddressClient = userAddressClient;
        this.orderDetailAssembler = orderDetailAssembler;
    }

    public OrderDetailResponse getMyOrderDetail(UUID orderId, Jwt jwt, Authentication authentication) {
        UUID currentUserId = extractCurrentUserId(jwt);
        boolean isAdmin = hasAdminRole(authentication);

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            throw new OrderAccessDeniedException("You are not allowed to view this order");
        }

        List<OrderItem> items = orderItemRepository.findByIdOrderId(orderId);
        Map<UUID, ProductSnapshot> products = enrichProducts(items);
        OrderDetailShippingAddressResponse shippingAddress = resolveShippingAddress(order);

        return orderDetailAssembler.toResponse(order, items, products, shippingAddress);
    }

    private Map<UUID, ProductSnapshot> enrichProducts(List<OrderItem> items) {
        Set<UUID> productIds = items.stream()
                .map(i -> i.getId().getProductId())
                .collect(Collectors.toSet());

        Map<UUID, ProductSnapshot> result = new LinkedHashMap<>();
        for (UUID productId : productIds) {
            try {
                ProductSnapshot snapshot = catalogClient.getProduct(productId);
                if (snapshot != null) {
                    result.put(productId, snapshot);
                }
            } catch (ExternalServiceException ex) {
                log.warn("Product enrichment failed for productId={} cause={}", productId, ex.getMessage());
            }
        }
        return result;
    }

    private OrderDetailShippingAddressResponse resolveShippingAddress(OrderEntity order) {
        OrderShippingAddress shippingAddress = order.getShippingAddress();
        if (shippingAddress == null) {
            return null;
        }

        if (hasAddressSnapshot(shippingAddress)) {
            return toShippingAddressResponse(
                    new AddressSnapshot(
                            shippingAddress.getId(),
                            shippingAddress.getFullName(),
                            shippingAddress.getPhone(),
                            shippingAddress.getProvinceName(),
                            shippingAddress.getProvinceName(),
                            shippingAddress.getDistrictName(),
                            shippingAddress.getWardName(),
                            shippingAddress.getAddressLine(),
                            null,
                            buildFullAddress(
                                    shippingAddress.getAddressLine(),
                                    shippingAddress.getWardName(),
                                    shippingAddress.getDistrictName(),
                                    shippingAddress.getProvinceName()),
                            null,
                            null));
        }

        UUID addressId = shippingAddress.getId();
        if (addressId == null) {
            return toShippingAddressResponse(
                    new AddressSnapshot(
                            null,
                            shippingAddress.getFullName(),
                            shippingAddress.getPhone(),
                            shippingAddress.getProvinceName(),
                            shippingAddress.getProvinceName(),
                            shippingAddress.getDistrictName(),
                            shippingAddress.getWardName(),
                            shippingAddress.getAddressLine(),
                            null,
                            buildFullAddress(
                                    shippingAddress.getAddressLine(),
                                    shippingAddress.getWardName(),
                                    shippingAddress.getDistrictName(),
                                    shippingAddress.getProvinceName()),
                            null,
                            null));
        }

        try {
            AddressSnapshot enriched = userAddressClient.getAddress(order.getUserId(), addressId);
            if (enriched != null) {
                return toShippingAddressResponse(enriched);
            }
        } catch (ExternalServiceException ex) {
            log.warn("Address enrichment failed for userId={} addressId={} cause={}",
                    order.getUserId(),
                    addressId,
                    ex.getMessage());
        }

        return toShippingAddressResponse(
                new AddressSnapshot(
                        shippingAddress.getId(),
                        shippingAddress.getFullName(),
                        shippingAddress.getPhone(),
                        shippingAddress.getProvinceName(),
                        shippingAddress.getProvinceName(),
                        shippingAddress.getDistrictName(),
                        shippingAddress.getWardName(),
                        shippingAddress.getAddressLine(),
                        null,
                        buildFullAddress(
                                shippingAddress.getAddressLine(),
                                shippingAddress.getWardName(),
                                shippingAddress.getDistrictName(),
                                shippingAddress.getProvinceName()),
                        null,
                        null));
    }

    private OrderDetailShippingAddressResponse toShippingAddressResponse(AddressSnapshot snapshot) {
        return new OrderDetailShippingAddressResponse(
                snapshot.id(),
                snapshot.recipientName(),
                snapshot.phoneNumber(),
                snapshot.province(),
                snapshot.city(),
                snapshot.district(),
                snapshot.ward(),
                snapshot.streetAddress(),
                snapshot.detailAddress(),
                snapshot.fullAddress(),
                snapshot.addressType(),
                snapshot.isDefault());
    }

    private boolean hasAddressSnapshot(OrderShippingAddress shippingAddress) {
        return isNotBlank(shippingAddress.getFullName())
                && isNotBlank(shippingAddress.getPhone())
                && isNotBlank(shippingAddress.getAddressLine());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String buildFullAddress(String... parts) {
        return String.join(", ",
                List.of(parts).stream().filter(this::isNotBlank).toList());
    }

    private UUID extractCurrentUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user subject");
        }
    }

    private boolean hasAdminRole(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ADMIN".equals(role));
    }
}
