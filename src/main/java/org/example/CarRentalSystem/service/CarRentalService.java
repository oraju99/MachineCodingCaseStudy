package org.example.CarRentalSystem.service;

import org.example.CarRentalSystem.enums.VehicleType;
import org.example.CarRentalSystem.models.Reservation;
import org.example.CarRentalSystem.models.Vehicle;
import org.example.CarRentalSystem.models.VehicleAvailability;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface CarRentalService {
    void addBranch(String id, String name);
    void addVehicle(String id, String branchId, VehicleType type, double pricePerHour);
    boolean hasOverlap(Vehicle vehicle, Instant start, Instant end);

    double calculatePrice(double pricePerHour, Instant start, Instant end);

    List<VehicleAvailability> searchAvailableVehicles(String branchId, VehicleType typeOrNull, Instant start, Instant end) throws Exception;

    Reservation holdVehicle(String customerId, String vehicleId, Instant start, Instant end, Duration ttl);

    void confirmBooking(String reservationId) throws Exception;

    void cancelBooking(String reservationId) throws Exception;

    void cleanExpiredReservations();
}
