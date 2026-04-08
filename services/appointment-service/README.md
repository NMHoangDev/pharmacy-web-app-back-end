# Appointment Service - Realtime Features

This service now supports real-time chat and WebRTC call signaling using WebSocket (STOMP).

## Database Migration
A new Flyway migration script `V2__add_chat_and_consultation.sql` has been added. Ensure this is applied to your database.

## REST API

### Chat History
`GET /api/appointments/{appointmentId}/messages?limit=20`

### Consultation (Call)
`POST /api/appointments/{appointmentId}/consultations`
Body: `{ "type": "VIDEO" }`

`POST /api/consultations/{sessionId}/ring`
`POST /api/consultations/{sessionId}/accept`
`POST /api/consultations/{sessionId}/end`

## WebSocket API

**Endpoint**: `/ws`
**Authorization**: Pass `Authorization: Bearer <token>` in the CONNECT headers.

### Stomp Topics (Subscribe)
- Chat: `/topic/appointments/{appointmentId}/chat`
- Call Events: `/topic/appointments/{appointmentId}/call`
- Call Signaling (WebRTC): `/topic/calls/{roomId}/signal`

### Stomp Destinations (Send)
- Send Chat: `/app/appointments/{appointmentId}/chat.send`
  - Payload: `{ "content": "Hello" }`

- Send Signal (Offer/Answer/Ice):
  - `/app/calls/{roomId}/signal.offer` -> Payload: `{ "sdp": "..." }`
  - `/app/calls/{roomId}/signal.answer` -> Payload: `{ "sdp": "..." }`
  - `/app/calls/{roomId}/signal.ice` -> Payload: `{ "candidate": "..." }`
  - Server broadcasts to `/topic/calls/{roomId}/signal` with type `OFFER`, `ANSWER`, or `ICE` and origin userId.

## Call Flow
1. User A calls `POST .../consultations` -> gets `sessionId`, `roomId`.
2. User A subscribes to `/topic/calls/{roomId}/signal` and `/topic/appointments/{apptId}/call`.
3. User A calls `POST .../ring`.
4. User B (subscribed to call topic) sees ringing event.
5. User B calls `POST .../accept`.
6. Both users start WebRTC logic (Create PeerConnection).
7. User A creates Offer -> sends to `/app/calls/{roomId}/signal.offer`.
8. User B receives Offer -> sets RemoteDesc -> creates Answer -> sends to `/app/calls/{roomId}/signal.answer`.
9. ICE Candidates exchanged via `/app/calls/{roomId}/signal.ice`.
