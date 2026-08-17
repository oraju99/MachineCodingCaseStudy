package org.example;

import org.example.inventoryManagement.controller.InventoryController;
import org.example.inventoryManagement.models.Product;
import org.example.inventoryManagement.models.Reservation;
import org.example.inventoryManagement.repository.InMemoryInventoryRepository;
import org.example.inventoryManagement.service.InventoryService;

import java.time.Duration;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        InMemoryInventoryRepository repo = new InMemoryInventoryRepository();
        InventoryService service = new InventoryService(repo);
        InventoryController controller = new InventoryController(service);

        // Background daemon scheduler polling every 100ms for expired reservations
        ScheduledExecutorService daemonScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        daemonScheduler.scheduleAtFixedRate(service::cleanExpiredReservations, 0, 100, TimeUnit.MILLISECONDS);

        // Onboarding test catalog items
        controller.addProduct("PROD_1", "PlayStation 5", 499.99, 10);

        System.out.println("--- Starting Concurrent Flash Sale Simulation (10 items, 15 competing users) ---");

        int numThreads = 15;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1); // Synchronized release gate to fire all requests simultaneously

        for (int i = 1; i <= numThreads; i++) {
            final String userId = "User_" + i;
            executor.submit(() -> {
                try {
                    latch.await(); // Wait for start signal
                    Reservation res = controller.reserveStock(userId, "PROD_1", 1, Duration.ofMillis(300));
                    System.out.println("SUCCESS: " + userId + " reserved stock! Reservation ID: " + res.getId());

                    // Simulate fast commit for User_1
                    if (userId.equals("User_1")) {
                        controller.commitReservation(res.getId());
                        System.out.println("PURCHASED: User_1 committed reservation successfully.");
                    }
                } catch (Exception e) {
                    System.out.println("REJECTED: " + userId + " - " + e.getMessage());
                }
            });
        }

        latch.countDown(); // Fire all 15 concurrent threads
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        Product p = repo.findProductById("PROD_1").get();
        System.out.println("\n--- Immediate Post-Sale State ---");
        System.out.println("Available Stock: " + p.getAvailableStock().get());
        System.out.println("Reserved Stock: " + p.getReservedStock().get());

        System.out.println("\nWaiting for uncommitted reservations to expire...");
        Thread.sleep(600); // Allow background daemon worker to reclaim expired reservations

        System.out.println("\n--- Final State After Reclaim ---");
        System.out.println("Available Stock: " + p.getAvailableStock().get() + " (9 returned after timeout)");
        System.out.println("Reserved Stock: " + p.getReservedStock().get());

        daemonScheduler.shutdown();
    }
}