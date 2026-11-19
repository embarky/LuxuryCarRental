package ch.unil.softarch.luxurycarrental.it;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * The whole system in Docker, started once for all integration tests:
 * <ul>
 *     <li>MySQL 8 (empty: the schema comes from the Flyway migrations of the service),</li>
 *     <li>Mailpit, to read the emails the application sends,</li>
 *     <li>Payara with the two WARs that Maven has just built, configured exactly like the
 *     production image (same asadmin scripts as the Dockerfile).</li>
 * </ul>
 */
final class TestEnvironment {

    static final String ADMIN_EMAIL = "admin@example.com";
    static final String ADMIN_PASSWORD = "admin-it-2026";

    private static final String PAYARA_IMAGE = "payara/server-full:6.2025.8-jdk17";

    static final Network NETWORK = Network.newNetwork();

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.43")
            .withNetwork(NETWORK)
            .withNetworkAliases("db")
            .withDatabaseName("luxury_car_rental_db")
            .withUsername("luxury_car_rental")
            .withPassword("it-password");

    static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:latest")
            .withNetwork(NETWORK)
            .withNetworkAliases("mailpit")
            .withExposedPorts(8025)
            .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

    static final GenericContainer<?> APP = new GenericContainer<>(appImage())
            .withNetwork(NETWORK)
            .withExposedPorts(8080)
            .withEnv("DB_HOST", "db")
            .withEnv("DB_PORT", "3306")
            .withEnv("DB_NAME", "luxury_car_rental_db")
            .withEnv("DB_USER", "luxury_car_rental")
            .withEnv("DB_PASSWORD", "it-password")
            .withEnv("API_BASE_URL", "http://localhost:8080/luxurycarrental/api")
            .withEnv("API_TOKEN_SECRET", "integration-test-secret-integration-test-secret")
            .withEnv("DEFAULT_ADMIN_EMAIL", ADMIN_EMAIL)
            .withEnv("DEFAULT_ADMIN_PASSWORD", ADMIN_PASSWORD)
            .withEnv("MAIL_SMTP_HOST", "mailpit")
            .withEnv("MAIL_SMTP_PORT", "1025")
            .withEnv("MAIL_SMTP_STARTTLS", "false")
            .dependsOn(MYSQL, MAILPIT)
            // ready when both applications answer (the web front end is deployed after the API)
            .waitingFor(new WaitAllStrategy(WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY)
                    .withStrategy(Wait.forHttp("/luxurycarrental/api/cars/search").forPort(8080).forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(4)))
                    .withStrategy(Wait.forHttp("/LuxuryCarRental/").forPort(8080).forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(1))))
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("payara")).withPrefix("payara"));

    private static boolean started;
    private static RuntimeException startFailure;

    private TestEnvironment() {
    }

    /** Starts everything once; if that failed, the next test classes fail at once instead of waiting again. */
    static synchronized void start() {
        if (startFailure != null) {
            throw new IllegalStateException("The test environment could not be started", startFailure);
        }
        if (!started) {
            try {
                APP.start();          // starts MySQL and Mailpit first
                started = true;
            } catch (RuntimeException e) {
                startFailure = e;
                throw e;
            }
        }
    }

    static String apiUrl() {
        return "http://" + APP.getHost() + ":" + APP.getMappedPort(8080) + "/luxurycarrental/api";
    }

    static String webUrl() {
        return "http://" + APP.getHost() + ":" + APP.getMappedPort(8080) + "/LuxuryCarRental";
    }

    static String mailpitUrl() {
        return "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025);
    }

    /** Same layout as the second stage of the project's Dockerfile, from the freshly built WARs. */
    private static ImageFromDockerfile appImage() {
        Path root = Path.of(System.getProperty("project.root", "..")).toAbsolutePath().normalize();
        Path service = root.resolve("service/target/luxurycarrental-service.war");
        Path webapp = root.resolve("webapp/target/luxurycarrental-webapp.war");
        Path driver = findDriver(root.resolve("service/target/libs"));
        return new ImageFromDockerfile("luxurycarrental-it", true)
                .withFileFromPath("service.war", service)
                .withFileFromPath("webapp.war", webapp)
                .withFileFromPath("mysql-driver.jar", driver)
                .withFileFromPath("pre-boot-commands.asadmin", root.resolve("docker/pre-boot-commands.asadmin"))
                .withFileFromPath("post-boot-commands.asadmin", root.resolve("docker/post-boot-commands.asadmin"))
                .withDockerfileFromBuilder(b -> b
                        .from(PAYARA_IMAGE)
                        .copy("mysql-driver.jar", "/opt/payara/appserver/glassfish/lib/mysql-driver.jar")
                        .copy("service.war", "/opt/payara/apps/luxurycarrental-service.war")
                        .copy("webapp.war", "/opt/payara/apps/luxurycarrental-webapp.war")
                        .copy("pre-boot-commands.asadmin", "/opt/payara/config/pre-boot-commands.asadmin")
                        .copy("post-boot-commands.asadmin", "/opt/payara/config/post-boot-commands.asadmin")
                        .build());
    }

    private static Path findDriver(Path libs) {
        try (Stream<Path> files = Files.list(libs)) {
            return files.filter(p -> p.getFileName().toString().startsWith("mysql-connector-j"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("MySQL driver not found in " + libs
                            + " – run 'mvn package' first"));
        } catch (IOException e) {
            throw new IllegalStateException("Build the project first ('mvn package'): " + libs + " is missing", e);
        }
    }
}
