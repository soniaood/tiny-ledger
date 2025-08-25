# Tiny Ledger

A tiny ledger API built with Spring Boot that supports deposits, withdrawals, and balance tracking with thread-safe operations.

## How to Run

### Prerequisites
- Java 17 or higher
- No additional software installation required

### Start the Application
```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## Features implemented

- **Record Transactions**: Deposits and withdrawals (money movements)
- **View Current Balance**: O(1) balance retrieval using atomic operations
- **View Transaction History**: Paginated transaction listing (newest first)
- **Functional web application**: REST APIs with no UI required
- **In-memory storage**: Keeping data in memory for simplicity
- **Input Validation**: Prevents invalid transactions (invalid amounts, insufficient funds)
- **Idempotency Support**: Optional idempotency key to prevent duplicate transactions
- **Thread Safety**: Concurrent operations supported
- **No external dependencies**: Runs without additional software installation

## API 
Complete API specification: [`spec.yaml`](src/main/resources/static/spec.yaml).

### 1. Record a transaction (deposit or withdrawal)
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":5000,"type":"DEPOSIT","description":"Initial deposit"}'
```

**Response:**
```json
{
  "id": 1,
  "type": "DEPOSIT",
  "amountInCents": "5000",
  "description": "Initial deposit",
  "createdOn": "2025-01-15T10:30:00Z"
}
```

### 2. Check Current Balance
```bash
curl http://localhost:8080/balance
```

**Response:**
```json
{
  "balanceInCents": "5000",
  "asOfTime": "2025-01-15T10:30:00Z"
}
```

### 3. View Transaction History
```bash
curl "http://localhost:8080/transactions?limit=5&offset=0"
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "type": "DEPOSIT",
      "amountInCents": "5000",
      "description": "Initial deposit",
      "createdOn": "2025-01-15T10:30:00Z"
    }
  ],
  "limit": 5,
  "offset": 0,
  "count": 1
}
```

### 4. Idempotent Transaction (prevents duplicates)
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{"amountInCents":"2000","type":"DEPOSIT","description":"Salary"}'
```

## Testing
### Unit and Integration Tests
```bash
# Run all tests
./gradlew test

# Run only integration tests
./gradlew test --tests "*Integration*"
```

### API Local Testing
A comprehensive test script is provided to test all API functionality with the application running locally:
- Balance checking and transaction recording
- Deposits, withdrawals, and transaction history
- Idempotency key functionality
- Input validation and error handling
- Pagination testing

```bash
# Run the application
./gradlew bootRun

# In a separate terminal, run the test script
chmod +x test-api.sh
./test-api.sh
```
_Note: Requires jq for JSON formatting._

## Design Decisions

### Money Precision
- Store all amounts as `long` in cents rather than using `BigDecimal` or `double`
- Avoids floating-point precision issues while keeping calculations simple and fast

### Thread Safety
- Service-level coordination: Uses ReentrantLock to ensure atomic business operations
- Concurrent data structures: `ConcurrentHashMap` for thread-safe storage operations
- Atomic balance tracking: `AtomicLong` for lock-free balance reads 
- Race condition prevention: Idempotency checks, balance validation, and transaction recording happen atomically

### Idempotency Support
- Added optional idempotency keys for duplicate prevention
- Real-world API consideration for real production concerns

### Balance Calculation Strategy
- Balance is maintained in real-time using AtomicLong for fast reads 
- Constant-time balance retrieval regardless of transaction volume

## Assumptions Made

1. **Single Account Model**: All transactions belong to one implicit account
2. **Single Currency**: All amounts are in the same currency (cents)
3. **No User Context**: Each request operates on the same ledger (no authentication needed as specified)
4. **Immediate Consistency**: Balance calculations happen in real-time

