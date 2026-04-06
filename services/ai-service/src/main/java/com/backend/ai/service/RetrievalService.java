package com.backend.ai.service;

import com.backend.ai.client.BranchClient;
import com.backend.ai.client.CatalogClient;
import com.backend.ai.client.InventoryClient;
import com.backend.ai.client.dto.BranchInternalResponse;
import com.backend.ai.client.dto.CatalogProductDto;
import com.backend.ai.client.dto.InventoryAvailabilityBatchItem;
import com.backend.ai.client.dto.InventoryAvailabilityBatchRequest;
import com.backend.ai.client.dto.InventoryAvailabilityBatchResponse;
import com.backend.ai.client.dto.InventoryAvailabilityByBranch;
import com.backend.ai.client.dto.InventoryItemQuantity;
import com.backend.ai.service.dto.KnowledgeHit;
import com.backend.ai.service.dto.RetrievalResult;
import com.backend.ai.service.dto.RetrievedProduct;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private static final List<String> STOP_WORDS = List.of(
            "thuoc", "thuốc", "cua", "của", "la", "là", "gi", "gì", "cho", "toi", "tôi", "ban", "bạn",
            "co", "có", "khong", "không", "hay", "va", "và", "the", "thể", "nao", "nào", "loai", "loại");

    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final BranchClient branchClient;
    private final KnowledgeBaseService knowledgeBaseService;

    public RetrievalService(
            CatalogClient catalogClient,
            InventoryClient inventoryClient,
            BranchClient branchClient,
            KnowledgeBaseService knowledgeBaseService) {
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.branchClient = branchClient;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public RetrievalResult retrieve(String question, UUID productId, UUID branchId) {
        String normalizedQuery = normalizeQuery(question);
        String foldedQuery = foldForSearch(normalizedQuery);
        boolean catalogSearchAllowed = shouldSearchCatalog(question, normalizedQuery, productId);
        log.info("retrieval started: question='{}', normalizedQuery='{}', productId={}, branchId={}",
                question, normalizedQuery, productId, branchId);

        List<CatalogProductDto> products = new ArrayList<>();
        boolean exactProductRequested = productId != null;

        if (productId != null) {
            try {
                CatalogProductDto detail = catalogClient.getPublicProduct(productId, branchId);
                if (detail != null) {
                    products.add(detail);
                }
            } catch (RuntimeException ex) {
                log.warn("catalog detail lookup failed for productId={}: {}", productId, ex.getMessage(), ex);
            }
        }

        if (catalogSearchAllowed && products.isEmpty() && StringUtils.hasText(normalizedQuery)) {
            products.addAll(searchCandidates(normalizedQuery, branchId));
        }

        if (catalogSearchAllowed && products.isEmpty()) {
            products.addAll(findFuzzyCandidates(question, normalizedQuery, foldedQuery, branchId));
        }

        List<CatalogProductDto> ranked = rankProducts(products, normalizedQuery, foldedQuery);
        Map<UUID, InventoryAvailabilityByBranch> aggregatedByProduct = aggregateAvailability(ranked);
        List<RetrievedProduct> retrievedProducts = ranked.stream()
                .limit(4)
                .map(product -> mapProduct(product, aggregatedByProduct.get(product.id())))
                .toList();

        boolean productFound = !retrievedProducts.isEmpty();
        boolean anyInStock = retrievedProducts.stream().anyMatch(product -> product.available() > 0);
        List<KnowledgeHit> knowledgeHits = knowledgeBaseService.search(question);
        log.info("retrieval completed: productFound={}, anyInStock={}, rankedCount={}, knowledgeHitCount={}",
                productFound, anyInStock, retrievedProducts.size(), knowledgeHits.size());

        return new RetrievalResult(
                normalizedQuery,
                exactProductRequested,
                productFound,
                anyInStock,
                retrievedProducts,
                knowledgeHits);
    }

    private List<CatalogProductDto> searchCandidates(String normalizedQuery, UUID branchId) {
        Set<String> attempts = new LinkedHashSet<>();
        if (StringUtils.hasText(normalizedQuery)) {
            attempts.add(normalizedQuery);
        }
        String probableName = extractProbableMedicineName(normalizedQuery);
        if (StringUtils.hasText(probableName)) {
            attempts.add(probableName);
        }
        List<String> tokens = tokenize(normalizedQuery);
        if (!tokens.isEmpty()) {
            attempts.add(String.join(" ", tokens));
            attempts.add(tokens.get(0));
            if (tokens.size() >= 2) {
                attempts.add(tokens.get(0) + " " + tokens.get(1));
            }
        }

        Map<UUID, CatalogProductDto> merged = new LinkedHashMap<>();
        for (String attempt : attempts) {
            try {
                List<CatalogProductDto> found = catalogClient.searchPublicProducts(attempt, branchId, 10);
                for (CatalogProductDto product : found) {
                    merged.putIfAbsent(product.id(), product);
                }
            } catch (RuntimeException ex) {
                log.warn("catalog search failed for query='{}': {}", attempt, ex.getMessage(), ex);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<CatalogProductDto> findFuzzyCandidates(String question, String normalizedQuery, String foldedQuery, UUID branchId) {
        List<CatalogProductDto> inventoryCatalog;
        try {
            inventoryCatalog = catalogClient.listPublicProducts(branchId, 200);
        } catch (RuntimeException ex) {
            log.warn("catalog broad fetch failed: {}", ex.getMessage(), ex);
            return List.of();
        }
        if (inventoryCatalog.isEmpty()) {
            return List.of();
        }

        List<String> tokens = tokenize(normalizedQuery);
        String probableName = foldForSearch(extractProbableMedicineName(question));
        return inventoryCatalog.stream()
                .filter(product -> fuzzyScore(product, tokens, foldedQuery, probableName) >= 4)
                .sorted(Comparator
                        .comparingInt((CatalogProductDto product) -> fuzzyScore(product, tokens, foldedQuery, probableName))
                        .reversed()
                        .thenComparing(CatalogProductDto::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(8)
                .toList();
    }

    private List<CatalogProductDto> rankProducts(List<CatalogProductDto> products, String normalizedQuery, String foldedQuery) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<String> tokens = tokenize(normalizedQuery);
        String probableName = foldForSearch(extractProbableMedicineName(normalizedQuery));
        return products.stream()
                .distinct()
                .sorted(Comparator
                        .comparingInt((CatalogProductDto product) -> scoreProduct(product, tokens, foldedQuery, probableName))
                        .reversed()
                        .thenComparing(CatalogProductDto::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private int scoreProduct(CatalogProductDto product, List<String> tokens, String foldedQuery, String probableName) {
        String haystack = foldForSearch(String.join(" ",
                safe(product.name()),
                safe(product.activeIngredient()),
                safe(product.indications()),
                safe(product.description())));
        String productName = foldForSearch(safe(product.name()));
        String activeIngredient = foldForSearch(safe(product.activeIngredient()));
        int score = 0;
        for (String token : tokens) {
            String foldedToken = foldForSearch(token);
            if (haystack.contains(foldedToken)) {
                score += 3;
            }
            if (productName.contains(foldedToken)) {
                score += 4;
            }
            if (activeIngredient.contains(foldedToken)) {
                score += 4;
            }
        }
        if (StringUtils.hasText(foldedQuery)) {
            if (productName.contains(foldedQuery) || activeIngredient.contains(foldedQuery)) {
                score += 10;
            }
            score += similarityBonus(productName, foldedQuery);
            score += similarityBonus(activeIngredient, foldedQuery);
        }
        if (StringUtils.hasText(probableName)) {
            score += similarityBonus(productName, probableName) + 2;
            score += similarityBonus(activeIngredient, probableName) + 2;
        }
        return Math.max(score, fuzzyScore(product, tokens, foldedQuery, probableName));
    }

    private int fuzzyScore(CatalogProductDto product, List<String> tokens, String foldedQuery, String probableName) {
        String productName = foldForSearch(safe(product.name()));
        String activeIngredient = foldForSearch(safe(product.activeIngredient()));
        int score = 0;
        if (StringUtils.hasText(probableName)) {
            score += similarityBonus(productName, probableName);
            score += similarityBonus(activeIngredient, probableName);
        }
        if (StringUtils.hasText(foldedQuery)) {
            score += similarityBonus(productName, foldedQuery);
            score += similarityBonus(activeIngredient, foldedQuery);
        }
        for (String token : tokens) {
            String foldedToken = foldForSearch(token);
            score += similarityBonus(productName, foldedToken);
            score += similarityBonus(activeIngredient, foldedToken);
        }
        return score;
    }

    private Map<UUID, InventoryAvailabilityByBranch> aggregateAvailability(List<CatalogProductDto> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        List<UUID> branchIds = branchClient.listActiveBranches().stream()
                .map(BranchInternalResponse::id)
                .toList();
        if (branchIds.isEmpty()) {
            return Map.of();
        }

        InventoryAvailabilityBatchRequest request = new InventoryAvailabilityBatchRequest(
                branchIds,
                products.stream().map(product -> new InventoryItemQuantity(product.id(), 1)).toList());

        InventoryAvailabilityBatchResponse response;
        try {
            response = inventoryClient.availabilityBatch(request);
        } catch (RuntimeException ex) {
            log.warn("inventory batch lookup failed: {}", ex.getMessage(), ex);
            return Map.of();
        }
        if (response == null || response.items() == null) {
            return Map.of();
        }

        Map<UUID, InventoryAvailabilityByBranch> aggregated = new HashMap<>();
        for (InventoryAvailabilityBatchItem item : response.items()) {
            int available = 0;
            int onHand = 0;
            int reserved = 0;
            if (item.byBranch() != null) {
                for (InventoryAvailabilityByBranch branch : item.byBranch()) {
                    available += branch.available();
                    onHand += branch.onHand();
                    reserved += branch.reserved();
                }
            }
            aggregated.put(item.productId(), new InventoryAvailabilityByBranch(item.productId(), available, onHand, reserved));
        }
        return aggregated;
    }

    private RetrievedProduct mapProduct(CatalogProductDto product, InventoryAvailabilityByBranch availability) {
        int available = availability == null ? 0 : availability.available();
        int onHand = availability == null ? 0 : availability.onHand();
        String stockStatus;
        if (availability == null) {
            stockStatus = "Ton kho chua xac dinh";
        } else if (available > 0) {
            stockStatus = "Con hang";
        } else if (onHand > 0) {
            stockStatus = "Dang giu hang";
        } else {
            stockStatus = "Hien dang het hang";
        }
        return new RetrievedProduct(
                product.id(),
                product.name(),
                product.slug(),
                product.price(),
                product.description(),
                product.dosageForm(),
                product.packaging(),
                product.activeIngredient(),
                product.indications(),
                product.usageDosage(),
                product.contraindicationsWarning(),
                product.otherInformation(),
                product.prescriptionRequired(),
                available,
                onHand,
                stockStatus);
    }

    private String normalizeQuery(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String lower = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        List<String> tokens = tokenize(lower);
        if (tokens.isEmpty()) {
            return lower;
        }
        return String.join(" ", tokens);
    }

    private String foldForSearch(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private List<String> tokenize(String input) {
        if (!StringUtils.hasText(input)) {
            return List.of();
        }
        return Arrays.stream(input.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .limit(8)
                .collect(Collectors.toList());
    }

    private String extractProbableMedicineName(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String normalized = foldForSearch(input);
        normalized = normalized
                .replaceAll("\\b(la thuoc gi|la gi|co tac dung gi|tac dung gi|tu van di|hay tu van di|giup toi|giup minh|cho toi biet|cho minh biet)\\b", " ")
                .replaceAll("\\b(thuoc|vien|sirup|siro|goi|hop|chai|dung|uoc|vi)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        List<String> tokens = Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.length() >= 3)
                .limit(4)
                .toList();
        return String.join(" ", tokens);
    }

    private int similarityBonus(String candidate, String query) {
        if (!StringUtils.hasText(candidate) || !StringUtils.hasText(query)) {
            return 0;
        }
        if (candidate.equals(query)) {
            return 12;
        }
        if (candidate.contains(query) || query.contains(candidate)) {
            return 8;
        }
        int distance = levenshtein(candidate, query);
        if (distance <= 1) {
            return 7;
        }
        if (distance <= 2) {
            return 5;
        }
        if (distance <= 3 && Math.abs(candidate.length() - query.length()) <= 3) {
            return 3;
        }
        return 0;
    }

    private int levenshtein(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean shouldSearchCatalog(String question, String normalizedQuery, UUID productId) {
        if (productId != null) {
            return true;
        }
        String folded = foldForSearch(question + " " + normalizedQuery);
        List<String> productSignals = List.of(
                "thuoc", "sản phẩm", "san pham", "hoat chat", "viên", "vien", "siro", "sirup",
                "capsule", "tablet", "mg", "ml", "uống", "uong", "liều", "lieu", "mua", "giá", "gia"
        );
        List<String> medicalTopicSignals = List.of(
                "viem", "xoang", "dau dau", "ho", "sot", "cam", "cum", "trieu chung", "benh",
                "la gi", "nguyen nhan", "dau hieu", "dieu tri", "điều trị", "triệu chứng", "nên làm gì"
        );
        boolean hasProductSignals = productSignals.stream().anyMatch(folded::contains);
        boolean hasMedicalSignals = medicalTopicSignals.stream().anyMatch(folded::contains);
        return hasProductSignals || !hasMedicalSignals;
    }
}
