package org.example.CarRentalSystem.controller;

import org.example.CarRentalSystem.enums.VehicleType;
import org.example.CarRentalSystem.models.Reservation;
import org.example.CarRentalSystem.models.VehicleAvailability;
import org.example.CarRentalSystem.service.CarRentalService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// REST API / Gateway Controller delegating requests down to core service layer
public class CarRentalController {
    private final CarRentalService service;

    public CarRentalController(CarRentalService service) {
        this.service = service;
    }

    public void addBranch(String id, String name) {
        service.addBranch(id, name);
    }

    public void addVehicle(String id, String branchId, VehicleType type, double pricePerHour) {
        service.addVehicle(id, branchId, type, pricePerHour);
    }

    public List<VehicleAvailability> searchAvailableVehicles(String branchId, VehicleType typeOrNull, Instant start, Instant end) throws Exception {
        return service.searchAvailableVehicles(branchId, typeOrNull, start, end);
    }

    public Reservation holdVehicle(String customerId, String vehicleId, Instant start, Instant end, Duration ttl) {
        return service.holdVehicle(customerId, vehicleId, start, end, ttl);
    }

    public void confirmBooking(String reservationId) throws Exception {
        service.confirmBooking(reservationId);
    }

    public void cancelBooking(String reservationId) throws Exception {
        service.cancelBooking(reservationId);
    }
}
