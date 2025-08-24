package tiny.ledger.service;

import tiny.ledger.entity.Movement;
import org.springframework.stereotype.Service;
import tiny.ledger.repository.LedgerRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final ReentrantLock lock = new ReentrantLock();

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public List<Movement> getMovementHistory(Integer limit,
                                             Integer offset) {
        return ledgerRepository.findMovements(limit, offset);
    }

    public Movement recordMovement(long amountInCents,
                                   Movement.MovementType type,
                                   String description,
                                   String idempotencyKey) {
        if (amountInCents <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero.");
        }

        lock.lock();
        try {
            if (type == Movement.MovementType.WITHDRAWAL &&
                ledgerRepository.getCurrentBalanceInCents() - amountInCents < 0) {
                throw new IllegalStateException("Insufficient funds for this transaction.");
            }

            if (idempotencyKey != null) {
                Optional<Long> existingId = ledgerRepository.findIdByIdempotencyKey(idempotencyKey);
                if (existingId.isPresent()) {
                    return ledgerRepository.findById(existingId.get())
                            .orElseThrow(() -> new IllegalStateException("Movement not found for idempotency key: " + idempotencyKey));
                }
            }

            Movement movement = new Movement(0L,
                                             type,
                                             amountInCents,
                                             Instant.now(),
                                             description,
                                             idempotencyKey
            );
            return ledgerRepository.save(movement);
        } finally {
            lock.unlock();
        }
    }

    public long getCurrentBalanceInCents() {
        return ledgerRepository.getCurrentBalanceInCents();
    }
}