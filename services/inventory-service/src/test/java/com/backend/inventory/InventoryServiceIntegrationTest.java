package com.backend.inventory;

import com.backend.inventory.api.dto.AdjustRequest;
import com.backend.inventory.api.dto.AdjustResponse;
import com.backend.inventory.api.dto.CommitRequest;
import com.backend.inventory.api.dto.ItemQuantity;
import com.backend.inventory.api.dto.ReserveRequest;
import com.backend.inventory.api.dto.ReserveResponse;
import com.backend.inventory.api.dto.StockDocumentApproveRequest;
import com.backend.inventory.api.dto.StockDocumentCreateRequest;
import com.backend.inventory.api.dto.StockDocumentLineRequest;
import com.backend.inventory.api.dto.StockDocumentResponse;
import com.backend.inventory.api.dto.StockDocumentSubmitRequest;
import com.backend.inventory.model.InventoryItem;
import com.backend.inventory.model.InventoryItemId;
import com.backend.inventory.model.StockDocumentStatus;
import com.backend.inventory.model.StockDocumentType;
import com.backend.inventory.repo.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class InventoryServiceIntegrationTest {

    private static final UUID DEFAULT_BRANCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mysqlProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void approveInAndOutStockDocumentUpdatesOnHand() {
        UUID productId = UUID.randomUUID();
        StockDocumentCreateRequest createIn = new StockDocumentCreateRequest(
                StockDocumentType.IN,
                "Supplier A",
                "SUP-1",
                "INV-001",
                "Restock",
                "admin",
                DEFAULT_BRANCH_ID,
                List.of(new StockDocumentLineRequest(
                        productId,
                        10,
                        new BigDecimal("12.50"),
                        "SKU-01",
                        "BATCH-01",
                        LocalDate.now().plusDays(30))));

        ResponseEntity<StockDocumentResponse> createResponse = restTemplate.postForEntity(
                "/api/inventory/admin/stock-documents", createIn, StockDocumentResponse.class);
        UUID docId = createResponse.getBody().id();

        restTemplate.postForEntity(
                "/api/inventory/admin/stock-documents/" + docId + "/submit",
                new StockDocumentSubmitRequest("admin"),
                StockDocumentResponse.class);

        ResponseEntity<StockDocumentResponse> approveResponse = restTemplate.postForEntity(
                "/api/inventory/admin/stock-documents/" + docId + "/approve",
                new StockDocumentApproveRequest("manager"),
                StockDocumentResponse.class);

        assertThat(approveResponse.getBody().status()).isEqualTo(StockDocumentStatus.APPROVED);

        InventoryItem afterIn = inventoryItemRepository
                .findById(new InventoryItemId(DEFAULT_BRANCH_ID, productId))
                .orElseThrow();
        assertThat(afterIn.getOnHand()).isEqualTo(10);

        StockDocumentCreateRequest createOut = new StockDocumentCreateRequest(
                StockDocumentType.OUT,
                "Supplier A",
                "SUP-1",
                "INV-002",
                "Customer order",
                "admin",
                DEFAULT_BRANCH_ID,
                List.of(new StockDocumentLineRequest(
                        productId,
                        4,
                        new BigDecimal("12.50"),
                        "SKU-01",
                        "BATCH-01",
                        LocalDate.now().plusDays(30))));

        ResponseEntity<StockDocumentResponse> createOutResponse = restTemplate.postForEntity(
                "/api/inventory/admin/stock-documents", createOut, StockDocumentResponse.class);
        UUID outId = createOutResponse.getBody().id();

        restTemplate.postForEntity(
                "/api/inventory/admin/stock-documents/" + outId + "/submit",
                new StockDocumentSubmitRequest("admin"),
                StockDocumentResponse.class);

        restTemplate.postForEntity(
                "/api/inventory/admin/stock-documents/" + outId + "/approve",
                new StockDocumentApproveRequest("manager"),
                StockDocumentResponse.class);

        InventoryItem afterOut = inventoryItemRepository
                .findById(new InventoryItemId(DEFAULT_BRANCH_ID, productId))
                .orElseThrow();
        assertThat(afterOut.getOnHand()).isEqualTo(6);
    }

    @Test
    void reserveAndCommitKeepInventoryConsistent() {
        UUID productId = UUID.randomUUID();
        AdjustRequest adjustRequest = new AdjustRequest(
                productId,
                12,
                "Seed inventory",
                "tester",
                null,
                null,
                null,
                null,
                null);
        restTemplate.postForEntity("/api/inventory/internal/inventory/adjust", adjustRequest, AdjustResponse.class);

        UUID orderId = UUID.randomUUID();
        ReserveRequest reserveRequest = new ReserveRequest(
                orderId,
                List.of(new ItemQuantity(productId, 5)),
                60,
                "Order reserve",
                "tester",
                null);

        ResponseEntity<ReserveResponse> reserveResponse = restTemplate.postForEntity(
                "/api/inventory/internal/inventory/reserve",
                reserveRequest,
                ReserveResponse.class);

        UUID reservationId = reserveResponse.getBody().reservationId();
        InventoryItem reservedItem = inventoryItemRepository
                .findById(new InventoryItemId(DEFAULT_BRANCH_ID, productId))
                .orElseThrow();
        assertThat(reservedItem.getReserved()).isEqualTo(5);

        CommitRequest commitRequest = new CommitRequest(reservationId, orderId, "Commit order", "tester", null);
        restTemplate.postForEntity("/api/inventory/internal/inventory/commit", commitRequest, ReserveResponse.class);

        InventoryItem committedItem = inventoryItemRepository
                .findById(new InventoryItemId(DEFAULT_BRANCH_ID, productId))
                .orElseThrow();
        assertThat(committedItem.getOnHand()).isEqualTo(7);
        assertThat(committedItem.getReserved()).isEqualTo(0);
    }
}
