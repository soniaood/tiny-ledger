package tiny.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(@Positive long amountInCents,
                                 String description,
                                 @NotBlank String type) {
}
