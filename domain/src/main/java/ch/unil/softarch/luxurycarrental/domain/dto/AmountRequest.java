package ch.unil.softarch.luxurycarrental.domain.dto;

import java.math.BigDecimal;

public class AmountRequest {
    public BigDecimal amount;

    public AmountRequest() {
    }

    public AmountRequest(BigDecimal amount) {
        this.amount = amount;
    }
}
