package org.example;

import org.example.CarRentalSystem.controller.CarRentalController;
import org.example.CarRentalSystem.enums.VehicleType;
import org.example.CarRentalSystem.models.Reservation;
import org.example.CarRentalSystem.repository.CarRentalRepository;
import org.example.CarRentalSystem.repository.InMemoryCarRentalRepository;
import org.example.CarRentalSystem.service.CarRentalService;
import org.example.CarRentalSystem.service.CarRentalServiceImpl;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        CarRentalRepository repo = new InMemoryCarRentalRepository();
        CarRentalService service = new CarRentalServiceImpl(repo);
        CarRentalController controller = new CarRentalController(service);

        ScheduledExecutorService daemonScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        daemonScheduler.scheduleAtFixedRate(service::cleanExpiredReservations, 0, 100, TimeUnit.MILLISECONDS);

        controller.addBranch("BR1", "Airport Branch");
        controller.addVehicle("V1", "BR1", VehicleType.SEDAN, 10.0);
        controller.addVehicle("V2", "BR1", VehicleType.SUV, 15.0);
        controller.addVehicle("V3", "BR1", VehicleType.HATCHBACK, 8.0);

        Instant t0 = Instant.now();

        System.out.println("--- Scenario 1: Sequential Non-Overlapping Bookings on Same Vehicle ---");
        try {
            Reservation r1 = controller.holdVehicle("Cust_1", "V1", t0, t0.plus(Duration.ofHours(1)), Duration.ofMinutes(5));
            System.out.println("SUCCESS: Held V1 [t0, t0+1h) -> Reservation " + r1.getId());
            controller.confirmBooking(r1.getId());
            System.out.println("SUCCESS: Confirmed reservation " + r1.getId());

            Reservation r2 = controller.holdVehicle("Cust_2", "V1", t0.plus(Duration.ofHours(1)), t0.plus(Duration.ofHours(2)), Duration.ofMinutes(5));
            System.out.println("SUCCESS: Held V1 [t0+1h, t0+2h) -> Reservation " + r2.getId());
            controller.confirmBooking(r2.getId());
            System.out.println("SUCCESS: Confirmed reservation " + r2.getId());
        } catch (Exception e) {
            System.out.println("REJECTED: " + e.getMessage());
        }

        System.out.println();
        System.out.println("--- Scenario 2: Overlapping Booking Is Rejected ---");
        try {
            Reservation r3 = controller.holdVehicle("Cust_3", "V1", t0.plus(Duration.ofMinutes(30)), t0.plus(Duration.ofMinutes(90)), Duration.ofMinutes(5));
            System.out.println("SUCCESS: Held V1 [t0+0.5h, t0+1.5h) -> Reservation " + r3.getId());
        } catch (Exception e) {
            System.out.println("REJECTED: Cust_3 - " + e.getMessage());
        }

        System.out.println();
        System.out.println("--- Scenario 3: Hold Expiration Releases Interval ---");
        try {
            Reservation r4 = controller.holdVehicle("Cust_4", "V2", t0, t0.plus(Duration.ofHours(1)), Duration.ofMillis(300));
            System.out.println("SUCCESS: Held V2 [t0, t0+1h) with 300ms TTL -> Reservation " + r4.getId());
        } catch (Exception e) {
            System.out.println("REJECTED: Cust_4 - " + e.getMessage());
        }

        try {
            Reservation r5 = controller.holdVehicle("Cust_5", "V2", t0.plus(Duration.ofMinutes(15)), t0.plus(Duration.ofMinutes(45)), Duration.ofMinutes(5));
            System.out.println("SUCCESS: Cust_5 held V2 overlapping slot -> Reservation " + r5.getId());
        } catch (Exception e) {
            System.out.println("REJECTED: Cust_5 - " + e.getMessage() + " (expected: hold still active)");
        }

        Thread.sleep(600);

        try {
            Reservation r6 = controller.holdVehicle("Cust_6", "V2", t0.plus(Duration.ofMinutes(15)), t0.plus(Duration.ofMinutes(45)), Duration.ofMinutes(5));
            System.out.println("SUCCESS: Cust_6 held V2 overlapping slot -> Reservation " + r6.getId() + " (expected: prior hold expired)");
        } catch (Exception e) {
            System.out.println("REJECTED: Cust_6 - " + e.getMessage());
        }

        System.out.println();
        System.out.println("--- Scenario 4: Concurrent Race - 10 Threads Competing for V3 Same Slot ---");
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        Instant raceStart = t0;
        Instant raceEnd = t0.plus(Duration.ofHours(1));

        for (int i = 1; i <= numThreads; i++) {
            final String customerId = "Customer_" + i;
            executor.submit(() -> {
                try {
                    latch.await();
                    Reservation r = controller.holdVehicle(customerId, "V3", raceStart, raceEnd, Duration.ofSeconds(30));
                    successCount.incrementAndGet();
                    System.out.println("SUCCESS: " + customerId + " held V3! Reservation ID: " + r.getId());
                } catch (Exception e) {
                    System.out.println("REJECTED: " + customerId + " - " + e.getMessage());
                }
            });
        }
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println();
        System.out.println("Total successful holds on V3 for the contested slot: " + successCount.get() + " (expected exactly 1)");

        daemonScheduler.shutdown();
    }
}
