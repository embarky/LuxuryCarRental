package ch.unil.softarch.luxurycarrental.domain.dto;

import ch.unil.softarch.luxurycarrental.domain.entities.Admin;

/** Answer to a successful administrator login: the API token and the administrator. */
public class AdminSession {
    public String token;
    public Admin admin;
}
