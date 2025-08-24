package tiny.ledger.dto;

import java.time.Instant;

public record TransactionResponse(
    long id,
    long amountInCents,
    Instant createdOn,
    String description,
    String type
) {
    public static TransactionResponse fromMovement(tiny.ledger.entity.Movement movement) {
        return new tiny.ledger.dto.TransactionResponse(
                movement.id(),
                movement.amountInCents(),
                movement.createdOn(),
                movement.description(),
                movement.type().name()
        );
    }
}
