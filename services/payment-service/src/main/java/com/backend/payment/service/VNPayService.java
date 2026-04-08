package com.backend.payment.service;

import com.backend.payment.api.dto.VnpayCreateRequest;
import com.backend.payment.api.dto.VnpayCreateResponse;
import com.backend.payment.api.dto.VnpayIpnResponse;
import com.backend.payment.config.VnpayProperties;
import com.backend.payment.model.PaymentProvider;
import com.backend.payment.model.PaymentStatus;
import com.backend.payment.model.PaymentTransaction;
import com.backend.payment.util.DateTimeUtil;
import com.backend.payment.util.HmacUtil;
import com.backend.payment.util.VnpayUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VNPayService {
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_TXN_REF_RETRY = 10;
    private static final int MIN_SUFFIX_DIGITS = 8;
    private static final int MAX_SUFFIX_DIGITS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final VnpayProperties properties;
    private final PaymentTransactionService transactionService;
    private final KafkaEventPublisher eventPublisher;
    private final RestClient orderServiceRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VNPayService(VnpayProperties properties,
            PaymentTransactionService transactionService,
            KafkaEventPublisher eventPublisher,
            RestClient orderServiceRestClient) {
        this.properties = properties;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.orderServiceRestClient = orderServiceRestClient;
    }

    public VnpayCreateResponse createPaymentUrl(VnpayCreateRequest request, String ipAddress) {
        String txnRef = generateTxnRef();
        ZoneId zoneId = resolveZoneId();
        String orderInfo = VnpayUtil.sanitizeOrderInfo(request.orderInfo());
        String normalizedIp = normalizeIp(ipAddress);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", properties.getVersion());
        vnpParams.put("vnp_Command", properties.getCommand());
        vnpParams.put("vnp_TmnCode", properties.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(request.amount() * 100L));
        vnpParams.put("vnp_CurrCode", properties.getCurrCode());
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", request.orderType() == null ? "other" : request.orderType());
        vnpParams.put("vnp_Locale", request.locale() == null ? properties.getLocaleDefault() : request.locale());
        vnpParams.put("vnp_ReturnUrl", properties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", normalizedIp);
        vnpParams.put("vnp_CreateDate", DateTimeUtil.nowVnpay(zoneId));
        vnpParams.put("vnp_ExpireDate", DateTimeUtil.plusMinutesVnpay(zoneId, properties.getExpireMinutes()));
        if (request.bankCode() != null && !request.bankCode().isBlank()) {
            vnpParams.put("vnp_BankCode", request.bankCode());
        }

        String hashData = VnpayUtil.buildHashData(vnpParams);
        String query = VnpayUtil.buildQueryString(vnpParams);
        String secretKey = properties.getHashSecret();
        if (secretKey != null) {
            secretKey = secretKey.trim();
        }
        String secureHash = HmacUtil.hmacSha512(secretKey, hashData);

        System.out.println("==== VNPAY DEBUG ====");
        System.out.println("TMN CODE: " + properties.getTmnCode());
        System.out.println("RETURN URL: " + properties.getReturnUrl());
        System.out.println("IPN URL: " + properties.getIpnUrl());
        int secretLen = secretKey == null ? 0 : secretKey.length();
        System.out.println("HASH SECRET LEN: " + secretLen);
        if (secretKey != null && secretLen >= 8) {
            System.out.println(
                    "HASH SECRET MASKED: " + secretKey.substring(0, 4) + "..." + secretKey.substring(secretLen - 4));
        }
        System.out.println("HASH DATA: " + hashData);
        System.out.println("QUERY: " + query);
        System.out.println("SECURE HASH: " + secureHash);
        System.out.println("=====================");

        String paymentUrl = properties.getUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
        transactionService.createPending(request.orderId(), PaymentProvider.VNPAY, txnRef, request.amount(),
                properties.getCurrCode());

        return new VnpayCreateResponse("00", "success", paymentUrl, txnRef);
    }

    public boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        String hashData = VnpayUtil.buildHashData(params);
        String secretKey = properties.getHashSecret();
        if (secretKey != null) {
            secretKey = secretKey.trim();
        }
        String computed = HmacUtil.hmacSha512(secretKey, hashData);
        return computed.equalsIgnoreCase(secureHash);
    }

    public VnpayIpnResponse handleIpn(Map<String, String> params) {
        if (!verifySignature(new HashMap<>(params))) {
            return new VnpayIpnResponse("97", "Invalid Checksum");
        }

        String txnRef = params.get("vnp_TxnRef");
        Optional<PaymentTransaction> txOptional = transactionService.findByTxnRef(txnRef);
        if (txOptional.isEmpty()) {
            return new VnpayIpnResponse("01", "Order not Found");
        }

        PaymentTransaction tx = txOptional.get();
        long expectedAmount = tx.getAmount() * 100;
        long actualAmount = Long.parseLong(params.getOrDefault("vnp_Amount", "0"));
        if (expectedAmount != actualAmount) {
            return new VnpayIpnResponse("04", "Invalid Amount");
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");
        String payDate = params.get("vnp_PayDate");
        Instant paidAt = DateTimeUtil.parseVnpayPayDate(payDate, resolveZoneId());
        String rawCallback = toJson(params);

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            transactionService.updateIfPending(txnRef, PaymentStatus.SUCCEEDED, responseCode,
                    transactionStatus, transactionNo, payDate, rawCallback, paidAt);

            boolean orderConfirmed = confirmOrderPaid(tx.getOrderId(), PaymentProvider.VNPAY.name(), txnRef,
                    tx.getAmount());
            if (!orderConfirmed) {
                return new VnpayIpnResponse("99", "Order confirmation failed");
            }

            eventPublisher.publishSucceeded(tx.getOrderId(), PaymentProvider.VNPAY.name(), txnRef, tx.getAmount(),
                    tx.getCurrency(), paidAt == null ? Instant.now() : paidAt);
            return new VnpayIpnResponse("00", "Confirm Success");
        }

        PaymentStatus status = "24".equals(responseCode) ? PaymentStatus.CANCELLED : PaymentStatus.FAILED;
        transactionService.updateIfPending(txnRef, status, responseCode, transactionStatus, transactionNo, payDate,
                rawCallback, null);
        eventPublisher.publishFailed(tx.getOrderId(), PaymentProvider.VNPAY.name(), txnRef, tx.getAmount(),
                tx.getCurrency(), responseCode);
        return new VnpayIpnResponse("00", "Confirm Success");
    }

    private boolean confirmOrderPaid(String orderId, String provider, String txnRef, long amount) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }
        final UUID orderUuid;
        try {
            orderUuid = UUID.fromString(orderId);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderUuid);
        body.put("provider", provider);
        body.put("transactionRef", txnRef);
        body.put("amount", (double) amount);

        try {
            orderServiceRestClient.post()
                    .uri("/api/order/pay")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            return false;
        }
    }

    private String generateTxnRef() {
        String datePrefix = DateTimeUtil.todayVnpayDate(resolveZoneId());
        for (int i = 0; i < MAX_TXN_REF_RETRY; i++) {
            int length = MIN_SUFFIX_DIGITS + RANDOM.nextInt(MAX_SUFFIX_DIGITS - MIN_SUFFIX_DIGITS + 1);
            String candidate = datePrefix + randomDigits(length);
            if (!transactionService.existsByTxnRef(candidate)) {
                return candidate;
            }
        }
        return datePrefix + digitsOnly(Long.toString(System.nanoTime()));
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append((char) ('0' + RANDOM.nextInt(10)));
        }
        return builder.toString();
    }

    private String digitsOnly(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private ZoneId resolveZoneId() {
        String timezone = properties.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE_ID;
        }
        String normalized = timezone.trim();
        if (normalized.startsWith("Etc/GMT+")) {
            return DEFAULT_ZONE_ID;
        }
        try {
            return ZoneId.of(normalized);
        } catch (RuntimeException ex) {
            return DEFAULT_ZONE_ID;
        }
    }

    private String normalizeIp(String ipAddress) {
        String candidate = ipAddress == null ? "" : ipAddress.trim();
        if (candidate.contains(",")) {
            candidate = candidate.split(",")[0].trim();
        }

        // VNPay expects IPv4 address in vnp_IpAddr. If we receive IPv6 (common on
        // localhost ::1),
        // fallback to IPv4 loopback to avoid provider-side normalization causing
        // signature mismatch.
        if (candidate.contains(":")) {
            return "127.0.0.1";
        }

        if (isValidIp(candidate)) {
            return candidate;
        }
        return isSandboxUrl() ? "127.0.0.1" : "127.0.0.1";
    }

    private boolean isSandboxUrl() {
        String url = properties.getUrl();
        if (url == null) {
            return false;
        }
        return url.toLowerCase(Locale.ROOT).contains("sandbox");
    }

    private boolean isValidIp(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        // VNPay: only accept IPv4 here
        if (value.contains(":")) {
            return false;
        }
        String[] parts = value.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) {
                    return false;
                }
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
