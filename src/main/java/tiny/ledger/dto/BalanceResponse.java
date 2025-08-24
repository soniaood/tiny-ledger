package tiny.ledger.dto;

import tiny.ledger.util.MoneyUtils;

import java.time.Instant;

public record BalanceResponse(
    String balanceInCents,
    Instant date) {

    public static BalanceResponse fromBalanceAtInstant(long balanceInCents, Instant asOfTime) {
        return new BalanceResponse(
                MoneyUtils.formatCents(balanceInCents),
                asOfTime
        );
    }
}
