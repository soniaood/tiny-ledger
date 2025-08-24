package tiny.ledger.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import tiny.ledger.dto.BalanceResponse;
import tiny.ledger.dto.ListResponse;
import tiny.ledger.dto.TransactionRequest;
import tiny.ledger.dto.TransactionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tiny.ledger.entity.Movement;
import tiny.ledger.service.LedgerService;

import java.time.Instant;
import java.util.List;

@RestController
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/transactions")
    public ListResponse<TransactionResponse> getTransactions(@RequestParam(required = false) Integer limit,
                                                             @RequestParam(required = false) Integer offset) {

        if (limit != null && limit <= 0 || offset != null && offset < 0) {
            throw new IllegalArgumentException("Invalid pagination parameters: limit must be > 0 and offset must be >= 0.");

        }
        List<Movement> movements = ledgerService.getMovementHistory(limit, offset);
        return new ListResponse<>(movements.stream().map(TransactionResponse::fromMovement).toList(),
                                          limit, offset, movements.size());
    }

    @PostMapping("/transactions")
    public TransactionResponse recordTransaction(@RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                                                 @Valid @RequestBody TransactionRequest request) {
        Movement movement = ledgerService.recordMovement(
                request.amountInCents(),
                Movement.MovementType.valueOf(request.type().toUpperCase()),
                request.description(),
                idemKey
        );
        return TransactionResponse.fromMovement(movement);
    }

    @GetMapping("/balance")
    public BalanceResponse getCurrentBalance() {
        return BalanceResponse.fromBalanceAtInstant(
                ledgerService.getCurrentBalanceInCents(),
                Instant.now()
        );
    }

}