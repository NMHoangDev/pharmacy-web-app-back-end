# discount-service

Production-ready microservice for managing discount campaigns and applying discount rules.

## Endpoints

### Admin

- `POST /admin/discounts/create` (ROLE_ADMIN)
- `PUT /admin/discounts/update/{id}` (ROLE_ADMIN)
- `DELETE /admin/discounts/{id}` (ROLE_ADMIN)
- `GET /admin/discounts/list` (ROLE_ADMIN)
- `PATCH /admin/discounts/toggle-status` (ROLE_ADMIN)

### User

- `GET /user/discounts/available` (ROLE_USER)
- `POST /user/discounts/validate` (ROLE_USER)
- `POST /user/discounts/apply` (ROLE_USER)

## Sample requests

### Create discount (admin)

```http
POST /admin/discounts/create
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "name": "Spring Sale",
  "code": "SPRING10",
  "type": "PERCENT",
  "value": 10,
  "maxDiscount": 50000,
  "minOrderValue": 200000,
  "usageLimit": 1000,
  "usagePerUser": 1,
  "startDate": "2026-03-01T00:00:00",
  "endDate": "2026-04-01T00:00:00",
  "status": "ACTIVE",
  "scopes": [
    { "scopeType": "ALL" }
  ]
}
```

### Validate discount (user)

```http
POST /user/discounts/validate
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "code": "SPRING10",
  "orderId": "ORDER-123",
  "order": {
    "subtotal": 300000,
    "shippingFee": 20000,
    "total": 320000,
    "items": [
      { "productId": 101, "categoryId": 5, "quantity": 2, "unitPrice": 150000 }
    ]
  }
}
```

Sample response (valid):

```json
{
  "valid": true,
  "reason": null,
  "discountAmount": 30000.00,
  "shippingDiscount": 0.00,
  "finalTotal": 290000.00,
  "discount": {
    "id": 1,
    "name": "Spring Sale",
    "code": "SPRING10",
    "type": "PERCENT",
    "value": 10.00,
    "maxDiscount": 50000.00,
    "minOrderValue": 200000.00,
    "usageLimit": 1000,
    "usagePerUser": 1,
    "usedCount": 0,
    "startDate": "2026-03-01T00:00:00",
    "endDate": "2026-04-01T00:00:00",
    "status": "ACTIVE",
    "createdAt": "2026-03-01T10:00:00",
    "scopes": [
      { "scopeType": "ALL", "scopeId": null }
    ],
    "targeted": false
  }
}
```

Sample response (invalid):

```json
{
  "valid": false,
  "reason": "Discount usage per user reached",
  "discountAmount": 0,
  "shippingDiscount": 0,
  "finalTotal": null,
  "discount": null
}
```

### Apply discount (user)

Same payload as validate, but hits `POST /user/discounts/apply`.

## Notes

- Keycloak roles are mapped from `realm_access.roles` / `resource_access.<client>.roles` via `common-security`.
- Kafka publish is best-effort: if Kafka is unavailable, APIs still succeed and log a warning.
- The order-event consumer is future-ready; it will act once order events include `promoCode`.

## Troubleshooting

### Port 7020 already in use

By default the service binds to port `7020` (override with `SERVER_PORT`). If you see: `Port 7020 was already in use`, either stop the process using that port or run on a different port.

Stop the process on Windows (PowerShell):

```powershell
Get-NetTCPConnection -LocalPort 7020 -State Listen | Select-Object -First 1 -ExpandProperty OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force }
```

Run on a different port (CMD):

```cmd
set SERVER_PORT=7021
mvn -DskipTests spring-boot:run
```

Run on a different port (Maven argument):

```cmd
mvn -DskipTests spring-boot:run -Dspring-boot.run.arguments=--server.port=7021
```
