package tiny.ledger.service;

import tiny.ledger.entity.Movement;
import org.springframework.stereotype.Service;
import tiny.ledger.repository.LedgerRepository;

import java.time.Instant;
import java.util.List;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;

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

        if (type == Movement.MovementType.WITHDRAWAL &&
            ledgerRepository.getCurrentBalanceInCents() - amountInCents < 0) {
            throw new IllegalStateException("Insufficient funds for this transaction.");
        }

        Movement movement = new Movement(0L,
                                         type,
                                         amountInCents,
                                         Instant.now(),
                                         description,
                                         idempotencyKey
        );

        return ledgerRepository.save(movement);
    }

    public long getCurrentBalanceInCents() {
        return ledgerRepository.getCurrentBalanceInCents();
    }
}