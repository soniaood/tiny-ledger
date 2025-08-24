package tiny.ledger.dto;

import java.time.Instant;

public record BalanceResponse(
    long balanceInCents,
    Instant date) {

    public static BalanceResponse fromBalanceAtInstant(long balanceInCents, Instant asOfTime) {
        return new BalanceResponse(balanceInCents, asOfTime);
    }
}
