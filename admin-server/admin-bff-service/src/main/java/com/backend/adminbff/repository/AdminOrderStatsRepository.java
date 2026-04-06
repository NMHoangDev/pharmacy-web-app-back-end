package com.backend.adminbff.repository;

import com.backend.adminbff.client.AdminOrderClient;
import com.backend.adminbff.dto.AdminOrderResponse;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AdminOrderStatsRepository {

    private final AdminOrderClient adminOrderClient;

    public AdminOrderStatsRepository(AdminOrderClient adminOrderClient) {
        this.adminOrderClient = adminOrderClient;
    }

    public List<AdminOrderResponse> findAllOrders() {
        return adminOrderClient.listOrders(null, null);
    }
}
