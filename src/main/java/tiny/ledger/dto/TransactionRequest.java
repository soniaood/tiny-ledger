package tiny.ledger.dto;

import jakarta.validation.constraints.NotBlank;

public record TransactionRequest(@NotBlank String amountInCents,
                                 String description,
                                 @NotBlank String type) {
}
