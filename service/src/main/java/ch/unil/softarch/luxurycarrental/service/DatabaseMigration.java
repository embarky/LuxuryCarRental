package ch.unil.softarch.luxurycarrental.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Brings the database schema up to date when the application starts.
 * <p>
 * The schema is defined by the versioned SQL scripts in {@code src/main/resources/db/migration}
 * ({@code V1__initial_schema.sql}, {@code V2__...}). Flyway records which scripts have run in the
 * table {@code flyway_schema_history} and only runs the new ones, so every environment ends up
 * with exactly the same schema. JPA does not create or change tables
 * ({@code schema-generation.database.action = none} in persistence.xml).
 * </p>
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)   // Flyway manages its own transactions
public class DatabaseMigration {

    private static final Logger LOG = Logger.getLogger(DatabaseMigration.class.getName());

    @Resource(lookup = "jdbc/LuxuryCarRental")
    private DataSource dataSource;

    @PostConstruct
    void migrate() {
        Flyway flyway = Flyway.configure(getClass().getClassLoader())
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        try {
            MigrateResult result = flyway.migrate();
            LOG.info("Database schema at version " + flyway.info().current().getVersion()
                    + " (" + result.migrationsExecuted + " migration(s) run now)");
        } catch (FlywayException e) {
            if (String.valueOf(e.getMessage()).contains("non-empty schema")) {
                LOG.severe("The database has tables but no Flyway history: it was created by an older "
                        + "version of this application. Start from an empty database, for example with "
                        + "'docker compose down -v && docker compose up -d --build'.");
            }
            throw e;
        }
    }
}
