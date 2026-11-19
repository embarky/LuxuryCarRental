package ch.unil.softarch.luxurycarrental.domain.model;

import ch.unil.softarch.luxurycarrental.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Helpers for amounts in CHF, always stored with two decimals. */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Money() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String amount) {
        return of(new BigDecimal(amount));
    }

    public static BigDecimal requirePositive(BigDecimal amount, String what) {
        if (amount == null || amount.signum() <= 0) {
            throw new DomainException(what + " must be greater than zero.");
        }
        return of(amount);
    }
}
