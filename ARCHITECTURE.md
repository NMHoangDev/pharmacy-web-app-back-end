# Pharmacy Microservices Architecture

## High-level Topology

```
[Front-end SPA]
     |
     v
[API Gateway] --(admin-only, optional)--> [Admin BFF]
     |                                      |\
     |                                      | \- [CMS Service]
     |                                      | \- [Reporting Service]
     |                                      | \- [Audit Service]
     |                                      | \- [Settings Service]
     v
+------------------- User Services -------------------+
| Catalog | Inventory | Order | Payment | User | Auth |
| Appointment | Review | Notification | Media         |
+-----------------------------------------------------+
     |
     +--> MySQL (per service DB)
     +--> Kafka (order.events, review.events, appointment.events, ...)
     +--> Object Storage (S3/MinIO) via Media Service
```

## Request Flow (User-facing)
- Client calls API Gateway (REST/JSON).
- Gateway routes to the appropriate user service.
- Service executes domain logic, reads/writes MySQL (service-specific schema), may publish events to Kafka.
- Notification service consumes events to send messages; Reporting service consumes events to build counters.
- Media uploads go to media-service, which stores files in S3/MinIO and returns URLs.
- Response is returned to client via Gateway.

## Request Flow (Admin)
- Admin front-end calls Admin BFF (recommended) or direct admin services.
- Admin BFF (when implemented) forwards to CMS/Reporting/Audit/Settings services and can enforce auth/RBAC.
- CMS manages banner/article/page content in MySQL (pharmacy_cms).
- Reporting consumes Kafka topics and serves aggregated metrics from pharmacy_reporting.
- Audit stores admin actions in pharmacy_audit.
- Settings manages key/value configs in pharmacy_settings.

## Data Stores
- MySQL: one schema per service (e.g., catalog_db, inventory_db, order_db, payment_db, appointment_db, review_db, user_db, plus admin schemas pharmacy_cms, pharmacy_reporting, pharmacy_audit, pharmacy_settings).
- Kafka: event bus for order/review/appointment and others; reporting/notification consume.
- Object storage (S3/MinIO): media assets via media-service.

## External Ports (current defaults)
- User services: ~8080-8088 range (catalog 8084, order 8083, user 8082, etc.).
- Admin services: CMS 8101, Reporting 8102, Audit 8103, Settings 8104.
- Gateway/Admin BFF: check respective configs (gateway port not shown here; admin-bff default 8099).

## Start-up Checklist
1) MySQL running; load schemas (admin-server/db.sql for admin, existing db.sql files for user services).
2) Kafka running at localhost:9092 (for reporting/notification event consumption).
3) Object storage configured for media-service (S3/MinIO credentials).
4) Build: `mvn clean install` at repository root.
5) Run gateway, user services, and admin services. Optionally run admin-bff if you want a single admin entrypoint.

## Call Paths Examples
- User catalog: GET /api/catalog/... -> catalog-service -> catalog_db.
- Create order: POST /api/orders -> order-service -> order_db -> publish order.events.
- Metrics: GET http://localhost:8102/api/admin/reporting/metrics -> reporting-service -> pharmacy_reporting.
- CMS banners: CRUD http://localhost:8101/api/admin/cms/banners -> pharmacy_cms.
- Audit log create: POST http://localhost:8103/api/admin/audit -> pharmacy_audit.
- Settings CRUD: http://localhost:8104/api/admin/settings -> pharmacy_settings.

## Notes
- DDL auto is disabled; ensure schemas are created before start.
- Admin-BFF endpoints are placeholders until wired to downstream services.
- Add authentication/authorization at gateway and/or BFF as needed for production.
