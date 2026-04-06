# Pharmacy App - Render.yaml Deployment Guide

## 📋 Overview

Phương án deployment này sử dụng `render.yaml` để deploy toàn bộ architecture với:

- **1 Gateway Service** (Public) - người dùng gọi vào
- **16+ Microservices** (Private) - chỉ gateway gọi nội bộ
- **Infrastructure Services**: MySQL, Kafka, Redis, Keycloak
- **Single render.yaml file** - deploy tất cả 1 lần

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────┐
│                   INTERNET / USERS                        │
└────────────────────┬─────────────────────────────────────┘
                     │ HTTPS (Port 443)
                     ▼
        ┌────────────────────────┐
        │  GATEWAY (PUBLIC)      │  Port: 8087
        │  pharmacy-gateway      │  Type: web
        └────────────┬───────────┘
                     │ INTERNAL RENDER NETWORK
        ┌────────────┴──────────────────────────────┐
        │                                            │
        ▼                                            ▼
┌──────────────────────────┐            ┌─────────────────────────┐
│ MICROSERVICES (PRIVATE)  │            │ INFRASTRUCTURE (PRIVATE)│
├──────────────────────────┤            ├─────────────────────────┤
│ • identity-service:7070  │            │ • MySQL         :3306   │
│ • user-service:7016      │            │ • Kafka         :9092   │
│ • catalog-service:7017   │            │ • Redis      (no port)  │
│ • inventory:7018         │            │ • Keycloak      :8080   │
│ • order-service:7019     │            │ • Config Server :8888   │
│ • payment-service:7020   │            └─────────────────────────┘
│ • ... 9 more services    │
└──────────────────────────┘
```

---

## 🚀 Step-by-Step Setup

### Step 1: Push render.yaml to GitHub

```bash
cd web-app/back-end/pharmacy-app
git add render.yaml scripts/build-*.sh .env.render.template README-Render.md
git commit -m "feat: add Render deployment configuration"
git push origin main
```

### Step 2: Create Render Project

1. Go to https://render.com
2. **New +** → **Sync GitHub Repository**
3. Select repo: `pharmacy-web-app-back-end`
4. Click **Create**

### Step 3: Add render.yaml

Render sẽ tự động nhận ra `render.yaml` hoặc:

1. Go to **Settings** → **Render Configuration**
2. Upload `web-app/back-end/pharmacy-app/render.yaml`
3. **Save**

### Step 4: Set Environment Variables

1. Go to **Settings** → **Environment Variables**
2. Add từ `.env.render.template`:

```
MYSQL_ROOT_PASSWORD=<generate-strong-password>
MYSQL_USER=pharma_user
MYSQL_PASSWORD=<generate-strong-password>
MYSQL_DATABASE=pharmacy_db
REDIS_PASSWORD=<generate-strong-password>
IDENTITY_JWT_SECRET=<base64-32+bytes>
KEYCLOAK_ADMIN_PASSWORD=<generate-strong-password>
SPRING_PROFILES_ACTIVE=docker,render,prod
```

### Step 5: Deploy

```bash
git push origin main  # Or click "Deploy" in Render UI
```

Render sẽ:

1. ✅ Build tất cả services từ monorepo
2. ✅ Start MySQL → Kafka → Redis → Keycloak
3. ✅ Start Config Server
4. ✅ Start Gateway (public access)
5. ✅ Start tất cả microservices (private)

---

## 📊 Services Overview

| Service                  | Port | Type       | Dependencies                  |
| ------------------------ | ---- | ---------- | ----------------------------- |
| **gateway**              | 8087 | **PUBLIC** | config-server, keycloak       |
| **identity-service**     | 7070 | private    | mysql, kafka, redis           |
| **user-service**         | 7016 | private    | mysql, kafka, redis, identity |
| **catalog-service**      | 7017 | private    | mysql, kafka, redis           |
| **inventory-service**    | 7018 | private    | mysql, kafka, redis           |
| **order-service**        | 7019 | private    | mysql, kafka, redis           |
| **payment-service**      | 7020 | private    | mysql, kafka                  |
| **media-service**        | 7021 | private    | mysql, kafka                  |
| **notification-service** | 7022 | private    | mysql, kafka                  |
| **review-service**       | 7023 | private    | mysql, kafka                  |
| **appointment-service**  | 7024 | private    | mysql, kafka                  |
| **pharmacist-service**   | 7025 | private    | mysql, kafka                  |
| **cart-service**         | 7026 | private    | redis                         |
| **branch-service**       | 7030 | private    | mysql, kafka                  |
| **content-service**      | 7031 | private    | mysql, kafka                  |
| **discount-service**     | 7035 | private    | mysql, kafka                  |
| **admin-bff-service**    | 7010 | private    | mysql, kafka                  |

---

## 🔧 Configuration Files

### render.yaml

Main deployment configuration with all services defined

### scripts/build-gateway.sh

Build gateway service only

### scripts/build-config-server.sh

Build config server

### scripts/build-service.sh

Generic build script cho microservices

### .env.render.template

Environment variables template để fill vào Render UI

---

## ✅ Testing & Monitoring

### Health Check URLs (after deploy)

Gateway (public):

```
https://<your-render-domain>.onrender.com/actuator/health
```

Check services logs:

```bash
# In Render UI: Select service → Logs tab
# Or via Render CLI:
render logs --service=pharmacy-gateway
render logs --service=pharmacy-catalog-service
```

### Common Issues & Fixes

| Issue                           | Cause                   | Fix                                     |
| ------------------------------- | ----------------------- | --------------------------------------- |
| Services can't connect to MySQL | DB not ready            | Add `depends_on` with health check      |
| Kafka connection timeout        | Wrong bootstrap-servers | Use service name: `pharmacy-kafka:9092` |
| JWT validation fails            | Wrong secret            | Check `IDENTITY_JWT_SECRET`             |
| 502 Bad Gateway                 | Service startup slow    | Increase `start_period` in healthCheck  |

---

## 💰 Cost Estimation

| Resource               | Plan     | Cost/Month      |
| ---------------------- | -------- | --------------- |
| Gateway (web)          | Standard | $25             |
| 1 Microservice (pserv) | Starter  | $7              |
| 16 Microservices       | Starter  | $112            |
| MySQL                  | Starter  | $7              |
| Kafka                  | Starter  | $7              |
| Redis                  | Starter  | $7              |
| Keycloak               | Starter  | $7              |
| Config Server          | Starter  | $7              |
| **TOTAL**              |          | **~$179/month** |

_Lưu ý: Render cung cấp **free tier** để start, upgrade khi cần._

---

## 🔄 Deployment Workflow

### Full Rebuild

```bash
git push origin main
# Render auto-detects changes, rebuilds all services
```

### Update Single Service

```bash
git add services/user-service/...
git commit -m "fix: user-service bug"
git push origin main
# Only user-service rebuilds (Render smart caching)
```

### Manual Redeploy

Render UI → Service → **Deploy** button

---

## 🛡️ Security Best Practices

1. **Never commit secrets**: Use `.env.render.template` as template only
2. **Environment Variables**: Store all secrets in Render UI, not in render.yaml
3. **Private Services**: Microservices chỉ accessible qua internal network
4. **Database**: Use Render's managed MySQL (automatic backups)
5. **SSL/TLS**: Render tự động cấp SSL certificate cho gateway

---

## 📝 Next Steps

1. ✅ Push render.yaml và scripts lên GitHub
2. ✅ Create Render project từ repository
3. ✅ Set environment variables
4. ✅ Click Deploy
5. ✅ Monitor logs
6. ✅ Test: `curl https://<domain>.onrender.com/actuator/health`
7. ✅ Integrate frontend (mobile-app, web-app/front-end)

---

## 🆘 Support

- Render CLI docs: https://render.com/docs/cli
- Spring Boot Docker: https://spring.io/guides/gs/spring-boot-docker
- Microservices networking: https://render.com/docs/networking
