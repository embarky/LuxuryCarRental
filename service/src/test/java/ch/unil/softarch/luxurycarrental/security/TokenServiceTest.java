package ch.unil.softarch.luxurycarrental.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private final TokenService tokens = new TokenService();

    @Test
    void issuedTokenIdentifiesTheCaller() {
        UUID id = UUID.randomUUID();
        Caller caller = tokens.verify(tokens.issue(Role.CUSTOMER, id)).orElseThrow();
        assertEquals(Role.CUSTOMER, caller.role());
        assertEquals(id, caller.id());
        assertFalse(caller.isAdmin());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokens.issue(Role.CUSTOMER, UUID.randomUUID());
        String forged = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("ADMIN|" + UUID.randomUUID() + "|9999999999").getBytes())
                + token.substring(token.indexOf('.'));
        assertTrue(tokens.verify(forged).isEmpty());
        assertTrue(tokens.verify("garbage").isEmpty());
    }

    @Test
    void tokenFromAnotherServerIsRejected() {
        String token = new TokenService().issue(Role.ADMIN, UUID.randomUUID());
        assertTrue(tokens.verify(token).isEmpty());
    }
}
