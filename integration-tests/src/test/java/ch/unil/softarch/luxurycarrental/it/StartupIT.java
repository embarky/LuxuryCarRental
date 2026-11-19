package ch.unil.softarch.luxurycarrental.it;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The server starts on an empty database and prepares everything itself. */
class StartupIT extends ApiTest {

    @Test
    void flywayCreatesTheSchemaFromTheMigrationScripts() throws Exception {
        List<String> versions = new ArrayList<>();
        try (Connection db = DriverManager.getConnection(TestEnvironment.MYSQL.getJdbcUrl(),
                TestEnvironment.MYSQL.getUsername(), TestEnvironment.MYSQL.getPassword());
             ResultSet rs = db.createStatement().executeQuery(
                     "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")) {
            while (rs.next()) {
                assertTrue(rs.getBoolean("success"), "migration " + rs.getString("version") + " failed");
                versions.add(rs.getString("version"));
            }
        }
        assertEquals(List.of("1", "2"), versions);
    }

    @Test
    void theFirstAdministratorIsCreatedFromTheEnvironment() {
        assertFalse(adminToken().isBlank());
    }

    @Test
    void theWebsiteIsServed() {
        Http.Response home = Http.download(TestEnvironment.webUrl() + "/", null);
        assertEquals(200, home.status());
        assertTrue(home.body().contains("Luxury Car Rental"));
    }
}
