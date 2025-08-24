package tiny.ledger.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {

    private MoneyUtils() {
    }

    public static String formatCents(long amountInCents) {
        return String.valueOf(amountInCents);
    }

    public static long parseCents(String amountInCents) {
        BigDecimal amount = new BigDecimal(amountInCents.trim());
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
