# Architecture decision records

Short notes on the decisions that shape this project: the situation, what was decided, and what it costs.

| # | Decision |
|---|---|
| [1](0001-shared-domain-module.md) | One domain module shared by the service and the web layer |
| [2](0002-preventing-double-bookings.md) | Preventing double bookings with a row lock on the car |
| [3](0003-stateless-signed-tokens.md) | Stateless signed tokens for API authentication |
| [4](0004-schema-migrations-with-flyway.md) | Database schema managed by Flyway migrations |
| [5](0005-checks-before-payment.md) | Identity and email checks before payment, not before sign-up |
