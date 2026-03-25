# Keycloak Setup for Pharmacy App

This directory contains the necessary configuration to run Keycloak 26 for authentication and authorization.

## How to Start

Navigate to this directory and run:

```bash
docker-compose up -d
```

Keycloak will be available at [http://localhost:8180](http://localhost:8180).
Admin Console credentials: `admin` / `admin`.

The realm `pharmacy` will be automatically imported on startup.

## Sample Users

| Username | Password | Role |
| :--- | :--- | :--- |
| `admin@local.test` | `Admin@123` | `ADMIN` |
| `pharmacist@local.test` | `Pharmacist@123` | `PHARMACIST` |
| `user@local.test` | `User@123` | `USER` |

## How to get Access Token (CURL)

To obtain a token via Password Grant (for development/testing):

```bash
curl -X POST http://localhost:8180/realms/pharmacy/protocol/openid-connect/token \
  -d "client_id=pharmacy-app" \
  -d "client_secret=G6Z8zQp4S9yV2W5vB7n3m1X0kC8jL5hR" \
  -d "username=admin@local.test" \
  -d "password=Admin@123" \
  -d "grant_type=password" \
  -d "scope=openid"
```

The JWT access token will contain the roles in the `realm_access.roles` claim.
