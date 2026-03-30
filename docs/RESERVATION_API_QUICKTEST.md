# Reservation API Quick Test

Tai lieu bo sung de dong bo voi module `/reservations` da duoc hoan thien.

## Base URL

- `http://localhost:9000`
- Header mac dinh: `Authorization: Bearer <ACCESS_TOKEN>`
- `Content-Type: application/json`

## 1) Check availability

`POST /reservations/availability`

```json
{
  "hotelId": 1,
  "checkInDate": "2026-04-10",
  "checkOutDate": "2026-04-12",
  "adults": 2,
  "children": 0,
  "roomTypeId": 1
}
```

## 2) Create reservation

`POST /reservations`

```json
{
  "hotelId": 1,
  "checkInDate": "2026-04-10",
  "checkOutDate": "2026-04-12",
  "adultCount": 2,
  "childCount": 0,
  "specialRequests": "High floor",
  "rooms": [
    {
      "roomTypeId": 1
    }
  ]
}
```

## 3) Get my reservations

`GET /reservations/me`

## 4) Update reservation status (STAFF/ADMIN)

`PATCH /reservations/{reservationId}/status`

```json
{
  "status": "CONFIRMED"
}
```

## 5) Add payment

`POST /reservations/payments`

```json
{
  "reservationId": 1,
  "amount": 500000,
  "method": "CASH",
  "note": "Deposit"
}
```

## 6) Add charge (STAFF/ADMIN)

`POST /reservations/charges`

```json
{
  "reservationId": 1,
  "chargeType": "MINIBAR",
  "description": "Soft drink",
  "amount": 120000
}
```

