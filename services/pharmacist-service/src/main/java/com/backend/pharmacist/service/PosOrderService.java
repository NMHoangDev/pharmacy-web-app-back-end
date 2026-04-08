package com.backend.pharmacist.service;

import com.backend.pharmacist.api.dto.pos.CancelOfflineOrderRequest;
import com.backend.pharmacist.api.dto.pos.ConfirmOfflinePaymentRequest;
import com.backend.pharmacist.api.dto.pos.CreateOfflineOrderRequest;
import com.backend.pharmacist.api.dto.pos.OfflineOrderItemResponse;
import com.backend.pharmacist.api.dto.pos.OfflineOrderPageResponse;
import com.backend.pharmacist.api.dto.pos.OfflineOrderResponse;
import com.backend.pharmacist.api.dto.pos.OfflinePaymentResponse;
import com.backend.pharmacist.api.dto.pos.PosOrderItemRequest;
import com.backend.pharmacist.api.dto.pos.PosProductSearchPageResponse;
import com.backend.pharmacist.api.dto.pos.PosProductSearchResponse;
import com.backend.pharmacist.api.dto.pos.RefundOfflineOrderRequest;
import com.backend.pharmacist.model.OfflineOrder;
import com.backend.pharmacist.model.OfflineOrderItem;
import com.backend.pharmacist.model.OfflineOrderPayment;
import com.backend.pharmacist.model.OfflineOrderStatus;
import com.backend.pharmacist.model.OfflinePaymentEventType;
import com.backend.pharmacist.model.OfflinePaymentMethod;
import com.backend.pharmacist.repo.OfflineOrderItemRepository;
import com.backend.pharmacist.repo.OfflineOrderPaymentRepository;
import com.backend.pharmacist.repo.OfflineOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class PosOrderService {

    private static final DateTimeFormatter ORDER_CODE_DATE = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OfflineOrderRepository orderRepository;
    private final OfflineOrderItemRepository itemRepository;
    private final OfflineOrderPaymentRepository paymentRepository;
    private final PosCatalogClient catalogClient;
    private final PosInventoryClient inventoryClient;
    private final ObjectMapper objectMapper;

    public PosOrderService(
            OfflineOrderRepository orderRepository,
            OfflineOrderItemRepository itemRepository,
            OfflineOrderPaymentRepository paymentRepository,
            PosCatalogClient catalogClient,
            PosInventoryClient inventoryClient,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PosProductSearchPageResponse searchProducts(String query, UUID branchId, int page, int size) {
        PosCatalogClient.ProductPage productPage = catalogClient.searchProducts(query, branchId, page, size);
        List<PosCatalogClient.ProductItem> products = productPage.content();
        if (products.isEmpty()) {
            return new PosProductSearchPageResponse(List.of(), 0, productPage.page(), productPage.size(),
                    productPage.totalPages());
        }

        List<PosInventoryClient.ItemQuantity> itemQuantities = products.stream()
                .map(item -> new PosInventoryClient.ItemQuantity(item.id(), 1))
                .toList();

        Map<UUID, PosInventoryClient.AvailabilityByBranch> availabilityMap = new HashMap<>();
        if (branchId != null) {
            PosInventoryClient.AvailabilityBatchResponse availability = inventoryClient.availabilityBatch(
                    new PosInventoryClient.AvailabilityBatchRequest(List.of(branchId), itemQuantities));

            if (availability != null && availability.items() != null) {
                for (PosInventoryClient.AvailabilityBatchItem row : availability.items()) {
                    if (row == null || row.productId() == null || row.byBranch() == null || row.byBranch().isEmpty()) {
                        continue;
                    }
                    PosInventoryClient.AvailabilityByBranch byBranch = row.byBranch().get(0);
                    availabilityMap.put(row.productId(), byBranch);
                }
            }
        }

        List<PosProductSearchResponse> content = products.stream().map(item -> {
            PosInventoryClient.AvailabilityByBranch stock = availabilityMap.get(item.id());
            String lot = extractAttribute(item.attributes(), "batchNo", "lotNo", "lot");
            String expiry = extractAttribute(item.attributes(), "expiryDate", "expDate");
            return new PosProductSearchResponse(
                    item.id(),
                    item.sku(),
                    item.name(),
                    item.imageUrl(),
                    toCurrencyLong(item.unitPrice()),
                    stock == null ? 0 : stock.available(),
                    stock == null ? 0 : stock.onHand(),
                    stock == null ? 0 : stock.reserved(),
                    lot,
                    expiry);
        }).toList();

        return new PosProductSearchPageResponse(
                content,
                productPage.totalElements(),
                productPage.page(),
                productPage.size(),
                productPage.totalPages());
    }

    public OfflineOrderResponse createOfflineOrder(CreateOfflineOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items is required");
        }

        OfflineOrder order = new OfflineOrder();
        order.setId(UUID.randomUUID());
        order.setOrderCode(generateOrderCode());
        order.setOrderType("OFFLINE");
        order.setBranchId(request.branchId());
        order.setPharmacistId(request.pharmacistId());
        order.setCustomerName(trimToNull(request.customerName()));
        order.setCustomerPhone(trimToNull(request.customerPhone()));
        order.setConsultationId(trimToNull(request.consultationId()));
        order.setNote(trimToNull(request.note()));

        long subtotal = 0L;
        List<OfflineOrderItem> items = new ArrayList<>();
        for (PosOrderItemRequest itemReq : request.items()) {
            validateItem(itemReq);
            long lineTotal = itemReq.unitPrice() * itemReq.qty();
            subtotal += lineTotal;

            OfflineOrderItem item = new OfflineOrderItem();
            item.setId(UUID.randomUUID());
            item.setOrderId(order.getId());
            item.setProductId(itemReq.productId());
            item.setSku(trimToNull(itemReq.sku()));
            item.setProductName(trimToNull(itemReq.productName()));
            item.setBatchNo(trimToNull(itemReq.batchNo()));
            item.setExpiryDate(itemReq.expiryDate());
            item.setQuantity(itemReq.qty());
            item.setUnitPrice(itemReq.unitPrice());
            item.setLineTotal(lineTotal);
            items.add(item);
        }

        long discount = safeMoney(request.discount());
        long taxFee = safeMoney(request.taxFee());
        long total = Math.max(0L, subtotal - discount + taxFee);

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTaxFee(taxFee);
        order.setTotalAmount(total);
        order.setStatus(OfflineOrderStatus.UNPAID);

        orderRepository.save(order);
        itemRepository.saveAll(items);

        return toOrderResponse(order, items, List.of());
    }

    public OfflineOrderResponse confirmPayment(UUID orderId, ConfirmOfflinePaymentRequest request) {
        OfflineOrder order = getOrderEntity(orderId);

        if (order.getStatus() == OfflineOrderStatus.CANCELLED || order.getStatus() == OfflineOrderStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not payable");
        }
        if (order.getStatus() == OfflineOrderStatus.PAID) {
            return getOrder(orderId);
        }

        List<OfflineOrderItem> items = itemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has no items");
        }

        long amountReceived = safeMoney(request.amountReceived());
        long total = safeMoney(order.getTotalAmount());
        if (request.method() == OfflinePaymentMethod.CASH && amountReceived < total) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount received is not enough");
        }

        PosInventoryClient.ReserveResponse reserve = inventoryClient.reserve(new PosInventoryClient.ReserveRequest(
                order.getId(),
                items.stream().map(item -> new PosInventoryClient.ItemQuantity(item.getProductId(), item.getQuantity()))
                        .toList(),
                900,
                "Offline POS reserve",
                request.pharmacistId().toString(),
                order.getBranchId()));

        if (reserve == null || reserve.reservationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventory reserve failed");
        }

        try {
            PosInventoryClient.ReserveResponse commit = inventoryClient.commit(new PosInventoryClient.CommitRequest(
                    reserve.reservationId(),
                    order.getId(),
                    "Offline POS commit",
                    request.pharmacistId().toString(),
                    order.getBranchId()));
            if (commit == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventory commit failed");
            }
        } catch (RuntimeException ex) {
            inventoryClient.release(new PosInventoryClient.ReleaseRequest(
                    reserve.reservationId(),
                    order.getId(),
                    "Rollback after commit failure",
                    request.pharmacistId().toString(),
                    order.getBranchId()));
            throw ex;
        }

        long change = Math.max(amountReceived - total, 0L);
        Instant now = Instant.now();

        order.setStatus(OfflineOrderStatus.PAID);
        order.setPaymentMethod(request.method());
        order.setAmountReceived(amountReceived);
        order.setChangeAmount(change);
        order.setTransferReference(trimToNull(request.transferReference()));
        order.setPaymentProofUrl(trimToNull(request.paymentProofUrl()));
        order.setInventoryReservationId(reserve.reservationId());
        order.setPaidAt(now);
        orderRepository.save(order);

        OfflineOrderPayment payment = new OfflineOrderPayment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(order.getId());
        payment.setEventType(OfflinePaymentEventType.PAID);
        payment.setMethod(request.method());
        payment.setAmount(total);
        payment.setAmountReceived(amountReceived);
        payment.setChangeAmount(change);
        payment.setTransferReference(trimToNull(request.transferReference()));
        payment.setProofUrl(trimToNull(request.paymentProofUrl()));
        payment.setNote(trimToNull(request.note()));
        payment.setCreatedBy(request.pharmacistId());
        paymentRepository.save(payment);

        return toOrderResponse(order, items, List.of(payment));
    }

    public OfflineOrderResponse cancelOrder(UUID orderId, CancelOfflineOrderRequest request) {
        OfflineOrder order = getOrderEntity(orderId);
        if (order.getStatus() == OfflineOrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Paid order cannot be cancelled");
        }
        if (order.getStatus() == OfflineOrderStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refunded order cannot be cancelled");
        }
        order.setStatus(OfflineOrderStatus.CANCELLED);
        if (StringUtils.hasText(request.reason())) {
            String note = trimToNull(request.reason());
            order.setNote(order.getNote() == null ? note : order.getNote() + " | Cancel reason: " + note);
        }
        orderRepository.save(order);
        return getOrder(orderId);
    }

    public OfflineOrderResponse refundOrder(UUID orderId, RefundOfflineOrderRequest request) {
        OfflineOrder order = getOrderEntity(orderId);
        if (order.getStatus() != OfflineOrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PAID order can be refunded");
        }

        List<OfflineOrderItem> items = itemRepository.findByOrderId(orderId);
        if (Boolean.TRUE.equals(request.restock())) {
            for (OfflineOrderItem item : items) {
                inventoryClient.restock(new PosInventoryClient.RestockRequest(
                        order.getId(),
                        order.getBranchId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getBatchNo(),
                        item.getExpiryDate(),
                        "Offline refund restock",
                        request.pharmacistId().toString()));
            }
        }

        order.setStatus(OfflineOrderStatus.REFUNDED);
        order.setRefundedAt(Instant.now());
        orderRepository.save(order);

        OfflineOrderPayment payment = new OfflineOrderPayment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(order.getId());
        payment.setEventType(OfflinePaymentEventType.REFUNDED);
        payment.setMethod(request.method() == null ? defaultMethod(order) : request.method());
        payment.setAmount(order.getTotalAmount());
        payment.setAmountReceived(order.getTotalAmount());
        payment.setChangeAmount(0L);
        payment.setTransferReference(trimToNull(request.transferReference()));
        payment.setProofUrl(trimToNull(request.paymentProofUrl()));
        payment.setNote(trimToNull(request.note()));
        payment.setCreatedBy(request.pharmacistId());
        paymentRepository.save(payment);

        return toOrderResponse(order, items, List.of(payment));
    }

    @Transactional(readOnly = true)
    public OfflineOrderResponse getOrder(UUID orderId) {
        OfflineOrder order = getOrderEntity(orderId);
        List<OfflineOrderItem> items = itemRepository.findByOrderId(orderId);
        List<OfflineOrderPayment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return toOrderResponse(order, items, payments);
    }

    @Transactional(readOnly = true)
    public OfflineOrderPageResponse listOrders(String range, OfflineOrderStatus status, UUID pharmacistId,
            UUID branchId,
            int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Instant from = resolveFrom(range);

        Specification<OfflineOrder> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (pharmacistId != null) {
                predicates.add(cb.equal(root.get("pharmacistId"), pharmacistId));
            }
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branchId"), branchId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        Page<OfflineOrder> orderPage = orderRepository.findAll(spec, pageable);
        List<OfflineOrderResponse> mapped = orderPage.getContent().stream().map(order -> {
            List<OfflineOrderItem> items = itemRepository.findByOrderId(order.getId());
            List<OfflineOrderPayment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
            return toOrderResponse(order, items, payments);
        }).toList();

        return new OfflineOrderPageResponse(mapped, orderPage.getTotalElements(), orderPage.getNumber(),
                orderPage.getSize(), orderPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public String printReceipt(UUID orderId) {
        OfflineOrderResponse order = getOrder(orderId);
        StringBuilder builder = new StringBuilder();
        builder.append("PHARMACY POS RECEIPT\n");
        builder.append("Order: ").append(order.orderCode()).append('\n');
        builder.append("Type: ").append(order.orderType()).append('\n');
        builder.append("Status: ").append(order.status()).append('\n');
        builder.append("Branch: ").append(order.branchId()).append('\n');
        builder.append("Pharmacist: ").append(order.pharmacistId()).append('\n');
        if (StringUtils.hasText(order.customerName())) {
            builder.append("Customer: ").append(order.customerName()).append('\n');
        }
        builder.append("----- ITEMS -----\n");
        for (OfflineOrderItemResponse item : order.items()) {
            builder.append(item.productName() == null ? item.productId() : item.productName())
                    .append(" x")
                    .append(item.qty())
                    .append(" @")
                    .append(item.unitPrice())
                    .append(" = ")
                    .append(item.lineTotal())
                    .append('\n');
        }
        builder.append("Subtotal: ").append(order.subtotal()).append('\n');
        builder.append("Discount: ").append(order.discount()).append('\n');
        builder.append("Tax/Fee: ").append(order.taxFee()).append('\n');
        builder.append("Total: ").append(order.totalAmount()).append('\n');
        return builder.toString();
    }

    private OfflineOrder getOrderEntity(UUID orderId) {
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offline order not found"));
    }

    private void validateItem(PosOrderItemRequest itemReq) {
        if (itemReq == null || itemReq.productId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item.productId is required");
        }
        if (itemReq.qty() == null || itemReq.qty() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item.qty must be > 0");
        }
        if (itemReq.unitPrice() == null || itemReq.unitPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item.unitPrice must be >= 0");
        }
    }

    private long safeMoney(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateOrderCode() {
        for (int i = 0; i < 10; i++) {
            String candidate = "POS" + LocalDateTime.now(VN_ZONE).format(ORDER_CODE_DATE)
                    + String.format("%03d", ThreadLocalRandom.current().nextInt(0, 1000));
            if (!orderRepository.existsByOrderCode(candidate)) {
                return candidate;
            }
        }
        return "POS" + System.currentTimeMillis();
    }

    private OfflineOrderResponse toOrderResponse(OfflineOrder order, List<OfflineOrderItem> items,
            List<OfflineOrderPayment> payments) {
        List<OfflineOrderItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(OfflineOrderItem::getProductName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(item -> new OfflineOrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getBatchNo(),
                        item.getExpiryDate(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()))
                .toList();

        List<OfflinePaymentResponse> paymentResponses = payments.stream().map(payment -> new OfflinePaymentResponse(
                payment.getId(),
                payment.getEventType(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getAmountReceived(),
                payment.getChangeAmount(),
                payment.getTransferReference(),
                payment.getProofUrl(),
                payment.getNote(),
                payment.getCreatedBy(),
                payment.getCreatedAt())).toList();

        return new OfflineOrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getOrderType(),
                order.getBranchId(),
                order.getPharmacistId(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getConsultationId(),
                order.getNote(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getTaxFee(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getAmountReceived(),
                order.getChangeAmount(),
                order.getTransferReference(),
                order.getPaymentProofUrl(),
                order.getInventoryReservationId(),
                order.getPaidAt(),
                order.getRefundedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses,
                paymentResponses);
    }

    private Instant resolveFrom(String range) {
        if (!StringUtils.hasText(range)) {
            return null;
        }
        String normalized = range.trim().toUpperCase();
        Instant now = Instant.now();
        if ("TODAY".equals(normalized)) {
            return LocalDate.now(VN_ZONE).atStartOfDay(VN_ZONE).toInstant();
        }
        if ("7D".equals(normalized) || "7_DAYS".equals(normalized) || "7DAY".equals(normalized)) {
            return now.minusSeconds(7L * 24 * 60 * 60);
        }
        return null;
    }

    private OfflinePaymentMethod defaultMethod(OfflineOrder order) {
        return order.getPaymentMethod() == null ? OfflinePaymentMethod.CASH : order.getPaymentMethod();
    }

    private Long toCurrencyLong(BigDecimal value) {
        if (value == null) {
            return 0L;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String extractAttribute(String attributes, String... keys) {
        if (!StringUtils.hasText(attributes) || keys == null || keys.length == 0) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(attributes);
            for (String key : keys) {
                JsonNode value = node.path(key);
                if (!value.isMissingNode() && !value.isNull()) {
                    String text = value.asText(null);
                    if (StringUtils.hasText(text)) {
                        return text;
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
