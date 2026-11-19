package ch.unil.softarch.luxurycarrental.security;

import ch.unil.softarch.luxurycarrental.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void hashMatchesOnlyTheRightPassword() {
        String hash = PasswordHasher.hash("secret123");
        assertTrue(hash.startsWith("pbkdf2_sha256$"));
        assertFalse(hash.contains("secret123"));
        assertTrue(PasswordHasher.matches("secret123", hash));
        assertFalse(PasswordHasher.matches("secret124", hash));
    }

    @Test
    void samePasswordGivesDifferentHashes() {
        assertNotEquals(PasswordHasher.hash("secret123"), PasswordHasher.hash("secret123"));
    }

    @Test
    void malformedHashesNeverMatch() {
        assertFalse(PasswordHasher.matches("secret123", "secret123"));
        assertFalse(PasswordHasher.matches("secret123", null));
    }

    @Test
    void policyNeedsEightCharactersWithLettersAndDigits() {
        assertThrows(DomainException.class, () -> PasswordHasher.checkPolicy("abc123"));
        assertThrows(DomainException.class, () -> PasswordHasher.checkPolicy("abcdefgh"));
        assertThrows(DomainException.class, () -> PasswordHasher.checkPolicy("12345678"));
        PasswordHasher.checkPolicy("customer123");
    }
}
