package tiny.ledger.repository;

import org.springframework.stereotype.Repository;
import tiny.ledger.entity.Movement;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class LedgerRepositoryImpl implements LedgerRepository {
    private final ConcurrentMap<Long, Movement> tinyLedger = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> idsByIdempotencyKey = new ConcurrentHashMap<>();

    private final AtomicLong idCounter = new AtomicLong();
    private final AtomicLong balanceInCents = new AtomicLong(0L);

    @Override
    public Optional<Movement> findById(long id) {
        return Optional.ofNullable(tinyLedger.get(id));
    }

    @Override
    public Optional<Long> findIdByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(idsByIdempotencyKey.get(idempotencyKey));
    }


    @Override
    public List<Movement> findMovements(Integer limit, Integer offset) {
        return tinyLedger.values().stream()
                .sorted(Comparator.comparing(Movement::createdOn).reversed())
                .skip(offset != null ? offset : 0)
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .toList();
    }

    @Override
    public Movement save(Movement movement) {
        long newId = idCounter.incrementAndGet();

        long currentBalance = balanceInCents.get();
        long balance = movement.type() == Movement.MovementType.DEPOSIT
                          ? currentBalance + movement.amountInCents()
                          : currentBalance - movement.amountInCents();
        balanceInCents.set(balance);

        Movement newMovement = new Movement(newId,
                                            movement.type(),
                                            movement.amountInCents(),
                                            Instant.now(),
                                            movement.description(),
                                            movement.idempotencyKey());
        tinyLedger.put(newId, newMovement);
        if (movement.idempotencyKey() != null) {
            idsByIdempotencyKey.put(movement.idempotencyKey(), newId);
        }

        return newMovement;
    }

    @Override
    public long getCurrentBalanceInCents() {
        return balanceInCents.get();
    }
}