# 4. Database schema managed by Flyway migrations

**Status:** accepted (replaces "JPA creates the tables at start-up")

## Context

At first, EclipseLink generated the tables from the entities when the application started. That is
convenient while nothing is stored, but it cannot change an existing table safely (adding a column,
moving data), and nobody can tell which schema a given database has.

## Decision

The schema is defined by versioned SQL scripts in `service/src/main/resources/db/migration`:

- `V1__initial_schema.sql`: the tables, keys, unique constraints, indexes for the availability
  search and `CHECK` constraints (balance never negative, return after pick-up);
- `V2__email_confirmation.sql`: a real change on existing data: a new column on `customer` and the
  replacement of the reset-code table by a general verification-code table.

At start-up a singleton EJB (`DatabaseMigration`) runs Flyway before anything else uses the database;
Flyway records the applied scripts in `flyway_schema_history` and runs only the new ones. JPA is set to
`schema-generation = none` and only maps the existing tables. Ids are generated in Java (UUID) instead
of by the database.

## Consequences

- Every environment (laptop, CI, a server) gets exactly the same schema, and each change is
  reviewed as SQL in the pull request.
- The integration tests start from an empty MySQL, so CI proves on every push that the scripts
  create a schema the entities work with.
- A changed entity needs a new migration script; forgetting it shows up in the integration tests.
- Databases created before Flyway was introduced have to be recreated once.
