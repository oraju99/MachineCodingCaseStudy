package org.example.inventoryManagement.service;

import org.example.inventoryManagement.enums.ReservationStatus;
import org.example.inventoryManagement.models.Product;
import org.example.inventoryManagement.models.Reservation;
import org.example.inventoryManagement.repository.InventoryRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class InventoryService {
    private final InventoryRepository repo;

    public InventoryService(InventoryRepository repo) {
        this.repo = repo;
    }

    public void addProduct(String id, String name, double price, int initialStock) {
        repo.saveProduct(new Product(id, name, price, initialStock));
    }

    public void restockProduct(String productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Product product = repo.findProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Non-blocking lock-free atomic stock increment
        product.getAvailableStock().addAndGet(quantity);
    }

    // High-concurrency fine-grained lock reservation protocol
    public Reservation reserveStock(String userId, String productId, int quantity, Duration ttl) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");

        Product product = repo.findProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Acquire lock specifically for this target product to eliminate cross-product contention
        product.getProductLock().lock();
        try {
            // Strict check-and-act: reject if available quantity is insufficient
            if (product.getAvailableStock().get() < quantity) {
                throw new IllegalStateException("Insufficient stock available for product: " + productId);
            }

            // Perform atomic inventory state adjustments
            product.getAvailableStock().addAndGet(-quantity);
            product.getReservedStock().addAndGet(quantity);

            Instant now = Instant.now();
            Reservation reservation = new Reservation(
                    UUID.randomUUID().toString(),
                    userId,
                    productId,
                    quantity,
                    now,
                    now.plus(ttl)
            );

            repo.saveReservation(reservation);
            return reservation;
        } finally {
            product.getProductLock().unlock(); // Guarantee lock release
        }
    }

    // Permanent conversion from reserved stock to completed order
    public void commitReservation(String reservationId) {
        Reservation reservation = repo.findReservationById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        // CAS atomic status transition ensuring double-commits are strictly prevented
        if (!reservation.getStatus().compareAndSet(ReservationStatus.PENDING, ReservationStatus.COMMITTED)) {
            throw new IllegalStateException("Reservation is no longer in PENDING state");
        }

        Product product = repo.findProductById(reservation.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Relinquish reserved counter metrics (item is now permanently sold)
        product.getReservedStock().addAndGet(-reservation.getQuantity());
    }

    // Manual cancellation by user or system
    public void cancelReservation(String reservationId) {
        Reservation reservation = repo.findReservationById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getStatus().compareAndSet(ReservationStatus.PENDING, ReservationStatus.CANCELLED)) {
            throw new IllegalStateException("Reservation cannot be cancelled from current state");
        }

        releaseStockBackToInventory(reservation);
    }

    // Background daemon sweep for expired active checkouts
    public void cleanExpiredReservations() {
        Instant now = Instant.now();
        for (Reservation reservation : repo.getAllReservations()) {
            if (reservation.getExpiresAt().isBefore(now)) {
                // Attempt atomic CAS transition to EXPIRED
                if (reservation.getStatus().compareAndSet(ReservationStatus.PENDING, ReservationStatus.EXPIRED)) {
                    releaseStockBackToInventory(reservation);
                    System.out.println(">>> Background Daemon: Expired reservation " + reservation.getId() + " and reverted stock.");
                }
            }
        }
    }

    // Reverts pending reserved quantity back into main available pool safely
    private void releaseStockBackToInventory(Reservation reservation) {
        Product product = repo.findProductById(reservation.getProductId()).orElse(null);
        if (product != null) {
            product.getProductLock().lock();
            try {
                product.getReservedStock().addAndGet(-reservation.getQuantity());
                product.getAvailableStock().addAndGet(reservation.getQuantity());
            } finally {
                product.getProductLock().unlock();
            }
        }
    }
}