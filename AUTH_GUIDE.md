# Pharmacy App - Authentication & Authorization Guide

This document explains how authentication and authorization are implemented in the Pharmacy App using Keycloak 26 and Spring Cloud Gateway.

## Architecture Overview

1.  **Keycloak**: Acts as the Identity Provider (IdP). Runs at `http://localhost:8180`. Realm: `pharmacy`.
2.  **Spring Cloud Gateway**: Acts as the Resource Server and entry point. Validates JWTs from the `Authorization` header.
3.  **Frontend**: React app that communicates with `identity-service` to login and receives a JWT, which it then uses for all subsequent API calls.

## Login Flow

1.  Frontend sends credentials to `/api/auth/login` (proxied to `identity-service`).
2.  `identity-service` performs a `password` grant flow with Keycloak.
3.  Keycloak returns an `access_token` (JWT).
4.  `identity-service` returns the `access_token` to the frontend as `token`.

## Token Storage & Usage

- **Storage**: The `access_token` is stored in `sessionStorage` under the key `authToken`.
- **Usage**: All API calls must include the header `Authorization: Bearer <access_token>`.
- **Intercepting**: The frontend uses a central `apiClient.js` (Axios instance) with an interceptor to automatically attach the token.

```javascript
// Example API call using apiClient
import apiClient from "./apiClient";

const getData = async () => {
    const res = await apiClient.get("/api/users/me");
    return res.data;
};
```

## Gateway Security Configuration

The Gateway is configured as an OAuth2 Resource Server.

- **Issuer URI**: `http://localhost:8180/realms/pharmacy`
- **JWKS Endpoint**: `{issuer}/protocol/openid-connect/certs` (fetched automatically by Spring).
- **Role Mapping**: Keycloak roles from `realm_access.roles` are mapped to Spring authorities with the `ROLE_` prefix (e.g., `ROLE_USER`, `ROLE_ADMIN`).
- **Audience Validation**: Relaxed to allow tokens even if the `aud` claim doesn't strictly match the expected resource ID (configured in `SecurityConfig.java`).

## Environment Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `KEYCLOAK_ISSUER_URI` | Full URL of the Keycloak realm | `http://localhost:8180/realms/pharmacy` |
| `REACT_APP_API_URL` | Base URL for the Gateway API | `http://localhost:8087` |

## Troubleshooting 401 Unauthorized

If you receive a 401 error:
1.  Check the Gateway logs (`org.springframework.security=DEBUG`).
2.  Verify the `iss` claim in the JWT matches the `issuer-uri` in `application.yml`.
3.  Ensure the token hasn't expired.
4.  Verify that the Frontend is indeed sending the `Authorization` header.
