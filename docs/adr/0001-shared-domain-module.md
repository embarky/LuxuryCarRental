# 1. One domain module shared by the service and the web layer

**Status:** accepted

## Context

The application has three layers: a JSF web front end, a REST service and a MySQL database. The web
layer must not access the database; it talks to the service over HTTP. Both sides need the same
notion of a car, a customer or a booking, and the same request and response formats.

## Decision

The entities, their business rules (the booking state machine, money handling, date ranges) and the
request/response DTOs live in one plain JAR, `domain`, used by both WARs.

- The service persists the entities with JPA and exposes them as JSON.
- The web layer deserialises the same classes from the API's JSON and uses their read-only logic
  (for example `booking.getAmountDue()` or `customer.isReadyToPay()`) to render pages.
- Every rule that changes state (`booking.pay()`, `customer.debit()`) is a method on the entity and is
  unit-tested without a server or database.

## Consequences

- One definition of the API contract: a renamed field fails at compile time on both sides.
- The rules cannot be bypassed by a service that forgets to call a check, because there is no setter
  that moves a booking to "paid".
- The web layer depends on JPA annotations it does not use (they are only metadata there).
- Both WARs must be deployed with the same version of the domain JAR. That is fine for one team and
  one deployment; independent teams would rather publish an OpenAPI contract and generate clients.
