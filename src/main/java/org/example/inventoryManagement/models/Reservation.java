package org.example.inventoryManagement.models;

import org.example.inventoryManagement.enums.ReservationStatus;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class Reservation {
    private final String id;
    private final String userId;
    private final String productId;
    private final int quantity;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final AtomicReference<ReservationStatus> status; // Atomic status transitions without heavy object locks

    public Reservation(String id, String userId, String productId, int quantity, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = new AtomicReference<>(ReservationStatus.PENDING);
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public AtomicReference<ReservationStatus> getStatus() { return status; }
}