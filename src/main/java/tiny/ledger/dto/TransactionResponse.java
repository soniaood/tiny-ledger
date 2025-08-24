package tiny.ledger.dto;

import tiny.ledger.util.MoneyUtils;

import java.time.Instant;

public record TransactionResponse(
    long id,
    String amountInCents,
    Instant date,
    String description,
    String type
) {
    public static TransactionResponse fromMovement(tiny.ledger.entity.Movement movement) {
        return new tiny.ledger.dto.TransactionResponse(
                movement.id(),
                MoneyUtils.formatCents(movement.amountInCents()),
                movement.createdOn(),
                movement.description(),
                movement.type().name()
        );
    }
}
