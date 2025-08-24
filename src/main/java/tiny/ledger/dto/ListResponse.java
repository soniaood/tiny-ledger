package tiny.ledger.dto;

import java.util.Collection;

public record ListResponse<T>(Collection<T> data,
                              Integer limit,
                              Integer offset) {
}
