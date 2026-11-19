# 2. Preventing double bookings with a row lock on the car

**Status:** accepted

## Context

Two customers may try to book the same car for overlapping dates at the same moment. Checking
"is there an overlapping booking?" and then inserting is a race: both requests can pass the check
before either inserts. A unique constraint cannot express "no overlapping date ranges" in MySQL.

## Decision

`BookingService.create` runs in one transaction that:

1. locks the car row with `PESSIMISTIC_WRITE` (`SELECT … FOR UPDATE`);
2. looks for bookings of this car that still block it (`PENDING`, `CONFIRMED`, `ACTIVE`) and overlap
   the requested range (`start < other.end AND other.start < end`, the return day is free again);
3. creates the booking, or answers `409 Conflict`.

Concurrent requests for the same car wait for the lock and then see the booking that was just
created. Requests for different cars do not block each other. `@Version` on `Car` and `Booking` adds
optimistic locking for other concurrent changes (for example an admin editing a car).

## Consequences

- Proven by an integration test: 10 simultaneous requests for the same car and dates give exactly one
  booking (`DoubleBookingIT`).
- Bookings of one car are serialised; at the scale of a rental company this is no bottleneck.
- The rule depends on the transaction isolation and locking of the database, so it is tested against
  a real MySQL and not an in-memory database.
