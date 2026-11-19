package ch.unil.softarch.luxurycarrental.domain;

import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void debitAndCreditMoveTheBalance() {
        Customer c = TestData.customer("1000.00", true);
        c.debit(new BigDecimal("250.50"));
        assertEquals(new BigDecimal("749.50"), c.getBalance());
        c.credit(new BigDecimal("0.50"));
        assertEquals(new BigDecimal("750.00"), c.getBalance());
    }

    @Test
    void balanceCannotGoNegative() {
        Customer c = TestData.customer("100.00", true);
        DomainException e = assertThrows(DomainException.class, () -> c.debit(new BigDecimal("100.01")));
        assertTrue(e.getMessage().contains("Insufficient balance"));
        assertEquals(new BigDecimal("100.00"), c.getBalance());
    }

    @Test
    void amountsMustBePositive() {
        Customer c = TestData.customer("100.00", true);
        assertThrows(DomainException.class, () -> c.debit(BigDecimal.ZERO));
        assertThrows(DomainException.class, () -> c.credit(new BigDecimal("-5")));
    }

    @Test
    void licenceValidity() {
        Customer c = TestData.customer("0", true);
        assertTrue(c.hasValidLicenceOn(LocalDate.of(2030, 1, 1)));
        assertFalse(c.hasValidLicenceOn(LocalDate.of(2030, 1, 2)));
    }

    @Test
    void ageIsComputedFromDateOfBirth() {
        Customer c = TestData.customer("0", true);
        c.setDateOfBirth(LocalDate.now().minusYears(30));
        assertEquals(30, c.getAge());
    }
}
