package ch.unil.softarch.luxurycarrental.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any logged-in user (customer or administrator) may call the endpoint; the method itself
 * checks that customers only touch their own data. Endpoints with neither this nor
 * {@link Public} are for administrators only.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LoggedIn {
}
