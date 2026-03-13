# Pharmacy App - Docker Compose (Phase 0)

## Scope
Compose này đóng gói các service sau:
- `gateway`
- `admin-bff-service`
- `appointment-service`, `branch-service`, `cart-service`, `catalog-service`, `content-service`
- `identity-service`, `inventory-service`, `media-service`, `notification-service`
- `order-service`, `payment-service`, `pharmacist-service`, `review-service`, `user-service`

Tất cả chạy cùng network `app-net`, profile `docker`, gọi nội bộ qua DNS service name.

## Run
Từ thư mục `web-app/back-end/pharmacy-app`:

```bash
docker compose up -d --build
```

Xem logs:

```bash
docker compose logs -f gateway
```

Dừng toàn bộ:

```bash
docker compose down
```

## Health checks
- Gateway (host):
  - `http://localhost:8087/actuator/health`
- Các service còn lại được healthcheck nội bộ qua `http://localhost:<internal-port>/actuator/health` trong từng container.

## Internal DNS test
Kiểm tra gateway gọi được service qua DNS nội bộ:

```bash
docker compose exec gateway wget -qO- http://user-service:7016/actuator/health
```

## Notes / Prerequisites
Phase 0 này giả định các hạ tầng phụ trợ đã có sẵn và truy cập được từ containers:
- MySQL (`mysql:3306`) cho các service có datasource
- Kafka (`kafka:9092`) cho các service có messaging
- Keycloak / OAuth2 issuer đúng với môi trường docker
- MinIO / SMTP / Redis nếu service đang dùng

Nếu các container infra không nằm trong compose này, cần đảm bảo DNS host tương ứng có thể resolve trong `app-net` hoặc override bằng env vars.
