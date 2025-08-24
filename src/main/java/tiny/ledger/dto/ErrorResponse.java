package tiny.ledger.dto;

public record ErrorResponse(ErrorCode code, String message) {
    public enum ErrorCode {
        INVALID_INPUT,
        INVALID_STATE,
        INTERNAL_ERROR
    }
}

