package tiny.ledger.repository;

import org.springframework.stereotype.Repository;
import tiny.ledger.entity.Movement;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRepository {
    Optional<Movement> findById(long id);
    Optional<Long> findIdByIdempotencyKey(String idempotencyKey);
    List<Movement> findMovements(Integer limit, Integer offset);
    Movement save(Movement movement);
    long getCurrentBalanceInCents();
}
