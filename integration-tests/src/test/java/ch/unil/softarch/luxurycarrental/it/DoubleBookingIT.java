package ch.unil.softarch.luxurycarrental.it;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/** A car can never be booked twice for the same days, not even by requests that arrive at the same moment. */
class DoubleBookingIT extends ApiTest {

    @Test
    void overlappingDatesAreRefused() {
        var car = newCar("300.00", "1000.00");
        TestCustomer first = register();
        TestCustomer second = register();
        LocalDate start = LocalDate.now().plusDays(10);
        assertEquals(200, book(first, car, start, start.plusDays(4)).status());

        assertEquals(409, book(second, car, start.plusDays(2), start.plusDays(6)).status());   // overlaps the end
        assertEquals(409, book(second, car, start.minusDays(1), start.plusDays(1)).status()); // overlaps the start
        assertEquals(409, book(second, car, start.plusDays(1), start.plusDays(2)).status());  // inside
        // Return day = next pick-up day is fine (the car comes back in the morning)
        assertEquals(200, book(second, car, start.plusDays(4), start.plusDays(5)).status());
    }

    @Test
    void parallelRequestsForTheSameCarGiveExactlyOneBooking() throws Exception {
        var car = newCar("300.00", "1000.00");
        int requests = 10;
        List<TestCustomer> customers = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            customers.add(register());
        }
        LocalDate start = LocalDate.now().plusDays(60);

        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        for (TestCustomer c : customers) {
            statuses.add(pool.submit(() -> {
                go.await();
                return book(c, car, start, start.plusDays(3)).status();
            }));
        }
        go.countDown();   // all at once
        List<Integer> results = new ArrayList<>();
        for (Future<Integer> f : statuses) {
            results.add(f.get());
        }
        pool.shutdown();

        assertEquals(1, results.stream().filter(s -> s == 200).count(), "statuses: " + results);
        assertEquals(requests - 1, results.stream().filter(s -> s == 409).count(), "statuses: " + results);
        assertEquals(1, bookingsInDatabase(car.getString("id")), "the database must hold exactly one booking");
    }

    private static int bookingsInDatabase(String carId) throws Exception {
        try (Connection db = DriverManager.getConnection(TestEnvironment.MYSQL.getJdbcUrl(),
                TestEnvironment.MYSQL.getUsername(), TestEnvironment.MYSQL.getPassword());
             PreparedStatement q = db.prepareStatement("SELECT COUNT(*) FROM booking WHERE car_id = ?")) {
            q.setString(1, carId);
            try (ResultSet rs = q.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
