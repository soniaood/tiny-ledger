# Tiny Ledger

A tiny ledger API built with Spring Boot that supports deposits, withdrawals, and balance tracking.

## How to Run

### Prerequisites
- Java 17 or higher

### Start the Application
```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## Features

- **Record Transactions**: Support for deposits and withdrawals
- **Balance Tracking**: Real-time current balance calculation
- **Transaction History**: Paginated list of all transactions (newest first)
- **Input Validation**: Prevents invalid transactions (zero amounts, insufficient funds)
- **Idempotency Support**: Optional idempotency key to prevent duplicate transactions
- **Thread Safety**: Concurrent operations supported

## API 
For complete API details and specification, refer to the API specification file: [`spec.yaml`](src/main/resources/static/spec.yaml).

### 1. Record a transaction (deposit or withdrawal)
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":"5000","type":"DEPOSIT","description":"Initial deposit"}'
```

**Response:**
```json
{
  "id": 1,
  "type": "DEPOSIT",
  "amountInCents": "5000",
  "description": "Initial deposit",
  "createdOn": "2024-01-15T10:30:00Z"
}
```

### 2. Check Current Balance
```bash
curl http://localhost:8080/balance
```

**Response:**
```json
{
  "balanceInCents": "4000",
  "asOfTime": "2024-01-15T10:35:00Z"
}
```

### 4. View Transaction History
```bash
curl "http://localhost:8080/transactions?limit=5&offset=0"
```

**Response:**
```json
{
  "data": [
    {
      "id": 2,
      "type": "WITHDRAWAL",
      "amountInCents": "1000",
      "description": "Coffee purchase",
      "createdOn": "2024-01-15T10:35:00Z"
    },
    {
      "id": 1,
      "type": "DEPOSIT",
      "amountInCents": "5000",
      "description": "Initial deposit",
      "createdOn": "2024-01-15T10:30:00Z"
    }
  ],
  "limit": 5,
  "offset": 0,
  "count": 2
}
```
## Implementation Notes

- All monetary amounts are handled in cents to avoid precision issues
- In-memory storage using thread-safe data structures
- RESTful API design with HTTP status codes
- Basic input validation and error handling

## Running Tests

```bash
./gradlew test
```