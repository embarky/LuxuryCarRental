# 5. Identity and email checks before payment, not before sign-up

**Status:** accepted

## Context

A rental company must know that a customer's email works (for confirmations and receipts) and that
their driving licence is real. Asking for both before an account exists makes signing up slow:
visitors who only want to look around leave.

## Decision

Signing up only checks the data itself (age 18+, licence still valid, unique email, phone and licence,
password policy). The customer can browse and reserve immediately. **Paying** requires both:

- the email address confirmed with the 6-digit code from the welcome email (valid 24 hours, a new
  code can be requested), and
- the licence checked by an administrator.

The rule is in the domain (`Booking.pay` refuses otherwise), so no client can skip it. The account page
shows the two steps as a checklist. Administrators can mark an email as confirmed when a customer
cannot receive the code (support override).

## Consequences

- No friction before the customer has a reason to continue, and the checks sit where the risk is:
  money and handing over a car.
- A reservation made before the checks may expire after 24 hours if the customer does not complete
  them in time; *My bookings* shows the payment deadline and what is still missing.
- Unconfirmed accounts can exist; they cannot pay and can be cleaned up.
