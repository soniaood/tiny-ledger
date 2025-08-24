package tiny.ledger.repository;

import org.springframework.stereotype.Repository;
import tiny.ledger.entity.Movement;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRepository {
    Movement save(Movement movement);
    List<Movement> findMovements(Integer limit, Integer offset);
    long getCurrentBalanceInCents();
}
