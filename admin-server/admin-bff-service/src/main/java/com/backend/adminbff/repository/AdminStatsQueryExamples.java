package com.backend.adminbff.repository;

/**
 * Query templates for moving stats aggregation into domain services/repositories.
 *
 * These are production-oriented SQL patterns used by User/Catalog/Order services when
 * implementing DB-side aggregate endpoints.
 */
public final class AdminStatsQueryExamples {

    private AdminStatsQueryExamples() {
    }

    public static final String USERS_STATUS_AGG = """
            SELECT
              COUNT(*) AS total_users,
              COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active_users,
              COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_users,
              COUNT(*) FILTER (WHERE status = 'BLOCKED') AS blocked_users,
              COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '7 day') AS new_users_last_7_days
            FROM users
            """;

    public static final String MEDICINES_STOCK_AGG = """
            SELECT
              COUNT(*) AS total_medicines,
              COUNT(*) FILTER (WHERE p.status = 'ACTIVE') AS active_medicines,
              COUNT(*) FILTER (WHERE i.available > 0 AND i.available <= :lowStockThreshold) AS low_stock_medicines,
              COUNT(*) FILTER (WHERE i.available <= 0) AS out_of_stock_medicines,
              COUNT(*) FILTER (WHERE p.status IN ('HIDDEN','INACTIVE','DISCONTINUED')) AS hidden_or_discontinued
            FROM catalog_products p
            LEFT JOIN inventory_items i ON i.product_id = p.id
            """;

    public static final String ORDERS_STATUS_REVENUE_AGG = """
            SELECT
              COUNT(*) AS total_orders,
              COUNT(*) FILTER (WHERE status IN ('DRAFT','PENDING_PAYMENT','PLACED')) AS pending_orders,
              COUNT(*) FILTER (WHERE status = 'SHIPPING') AS shipping_orders,
              COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_orders,
              COUNT(*) FILTER (WHERE status IN ('CANCELED','CANCELLED')) AS cancelled_orders,
              COALESCE(SUM(CASE WHEN status = 'COMPLETED' AND created_at >= CURRENT_DATE THEN total_amount END), 0) AS revenue_today,
              COALESCE(SUM(CASE WHEN status = 'COMPLETED' AND created_at >= CURRENT_DATE - INTERVAL '6 day' THEN total_amount END), 0) AS revenue_this_week
            FROM orders
            """;
}
