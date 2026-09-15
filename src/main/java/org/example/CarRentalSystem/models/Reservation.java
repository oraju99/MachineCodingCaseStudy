package org.example.CarRentalSystem.models;

import lombok.Data;
import org.example.CarRentalSystem.enums.ReservationStatus;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Data
public class Reservation {
    private final String id;
    private final String vehicleId;
    private final String customerId;
    private final Instant startTime;
    private final Instant endTime;
    private final double totalPrice;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final AtomicReference<ReservationStatus> reservationStatus;

    public Reservation(String id, String vehicleId, String customerId, Instant startTime, Instant endTime, double totalPrice, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.reservationStatus = new AtomicReference<>(ReservationStatus.HELD);
    }
}
