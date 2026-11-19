package ch.unil.softarch.luxurycarrental.domain.dto;

import ch.unil.softarch.luxurycarrental.domain.entities.Customer;

/** Answer to a successful customer login: the API token and the customer. */
public class CustomerSession {
    public String token;
    public Customer customer;
}
