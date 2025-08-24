# Tiny Ledger

A tiny ledger API built with Spring Boot that supports deposits, withdrawals, and balance tracking.

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

- **Record Transactions**: Support for deposits and withdrawals (money movements)
- **View Current Balance**: High-performance balance retrieval 
- **View Transaction History**: Paginated list of all transactions (newest first)
- **Functional web application**: REST APIs with no UI required
- **In-memory storage**: Keeping data in memory using `ConcurrentHashMap` for simplicity
- **Input Validation**: Prevents invalid transactions (negative amounts, zero amounts, insufficient funds)
- **Idempotency Support**: Optional idempotency key to prevent duplicate transactions
- **Thread Safety**: Concurrent operations supported
- **No external dependencies**: Runs without additional software installation
  
## API 
For complete API details and specification, refer to the API specification file: [`spec.yaml`](src/main/resources/static/spec.yaml).

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

## Running Tests

```bash
./gradlew test
```

## Architecture & Design Decisions

### Money Precision
- Store all amounts as `long` in cents rather than using `BigDecimal` or `double`
- Avoids floating-point precision issues while keeping calculations simple and fast

### API Design
- RESTful endpoints with proper HTTP verbs and status codes

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
5. **Simple Validation**: Basic business rules (no zero amounts, no overdrafts)

## Performance Characteristics
### Current Performance Profile

- Transaction Recording: Serialized writes with ReentrantLock for consistency 
- Balance Retrieval: O(1) - Instant response via cached AtomicLong 
- Transaction History: O(N log N) - For sorting, plus O(limit) for pagination 
- Memory Usage: O(N) - Linear with transaction count 
- Read Performance: Optimized for high-frequency balance checks

### Scaling Considerations

- Read-heavy workloads: Optimized with O(1) balance operations 
- Write-heavy workloads: Serialized writes ensure consistency at cost of throughput 
- High-frequency balance checks: Perform exceptionally well with cached balance strategy 
- Large transaction volumes: Memory usage scales linearly, balance performance remains constant

## Technical Implementation

### Layer Architecture
```
Controller → Service → Repository
```

- **Controller**: HTTP handling, request/response mapping, input validation
- **Service**: Business logic, validation rules, transaction orchestration
- **Repository**: Data access abstraction, thread-safe operations

### Key Classes
- `LedgerController`: REST endpoints
- `LedgerService`: Business logic and validation
- `LedgerRepository`: In-memory data operations
- `Movement`: Immutable transaction record

## Time Investment & Trade-offs

This implementation balances:
- **Functionality**: Required features implemented
- **Code Quality**: Clean, maintainable architecture
- **Performance**: Optimized O(1) balance reads with strategic write serialization
- **Real-world Awareness**: Included considerations like idempotency and thread safety 
- **Thread Safety**: Robust concurrency handling with ReentrantLock coordination 
- **Scalability**: Designed to handle high-frequency balance checks efficiently