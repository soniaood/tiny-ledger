package tiny.ledger.entity;

import java.time.Instant;

public record Movement(long id,
                       MovementType type,
                       long amountInCents,
                       Instant createdOn,
                       String description,
                       String idempotencyKey) {

    public enum MovementType {
        DEPOSIT,
        WITHDRAWAL
    }
}
