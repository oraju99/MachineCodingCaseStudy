package org.example.CarRentalSystem.service;

import org.example.CarRentalSystem.enums.ReservationStatus;
import org.example.CarRentalSystem.enums.VehicleType;
import org.example.CarRentalSystem.models.Branch;
import org.example.CarRentalSystem.models.Reservation;
import org.example.CarRentalSystem.models.Vehicle;
import org.example.CarRentalSystem.models.VehicleAvailability;
import org.example.CarRentalSystem.repository.CarRentalRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class CarRentalServiceImpl implements CarRentalService {
    private final CarRentalRepository carRentalRepository;

    public CarRentalServiceImpl(CarRentalRepository carRentalRepository) {
        this.carRentalRepository = carRentalRepository;
    }

    @Override
    public void addBranch(String id, String name) {
        Branch branch = new Branch(id, name);
        carRentalRepository.saveBranch(branch);
    }

    @Override
    public void addVehicle(String id, String branchId, VehicleType type, double pricePerHour) {
        Vehicle vehicle = new Vehicle(id, branchId, type, pricePerHour);
        carRentalRepository.saveVehicle(vehicle);
    }

    @Override
    public boolean hasOverlap(Vehicle vehicle, Instant start, Instant end) {
        for (Reservation r : vehicle.getReservationList()) {
            ReservationStatus status = r.getReservationStatus().get();
            if (status != ReservationStatus.HELD && status != ReservationStatus.CONFIRMED) continue;
            if (start.isBefore(r.getEndTime()) && r.getStartTime().isBefore(end)) return true;
        }
        return false;
    }

    @Override
    public double calculatePrice(double pricePerHour, Instant start, Instant end) {
        long seconds = Duration.between(start, end).getSeconds();
        long billableHours = (seconds + 3599) / 3600; // ceiling division
        return billableHours * pricePerHour;
    }

    @Override
    public List<VehicleAvailability> searchAvailableVehicles(String branchId, VehicleType typeOrNull, Instant start, Instant end) throws Exception {
        Branch branch = carRentalRepository.findBranchById(branchId).orElseThrow( () -> new Exception("Branch not found for branchId") );
        List<Vehicle> vehicleList = carRentalRepository.getAllVehicles();
        List<VehicleAvailability> vehicleAvailabilityList = new ArrayList<>();
        for(Vehicle vehicle : vehicleList) {
            if (!vehicle.getBranchId().equals(branchId)) continue;
            if (typeOrNull != null && vehicle.getVehicleType() != typeOrNull) continue;

            vehicle.getVehicleLock().lock();
            try {
                if (!hasOverlap(vehicle, start, end)) {
                    VehicleAvailability vehicleAvailability = new VehicleAvailability(
                            vehicle.getId(),
                            branchId,
                            vehicle.getVehicleType(),
                            vehicle.getPricePerHour(),
                            calculatePrice(vehicle.getPricePerHour(), start, end));
                    vehicleAvailabilityList.add(vehicleAvailability);
                }
            } finally {
                vehicle.getVehicleLock().unlock();
            }
        }

        return vehicleAvailabilityList;
    }

    @Override
    public Reservation holdVehicle(String customerId, String vehicleId, Instant start, Instant end, Duration ttl) {
        if (start == null || end == null || !start.isBefore(end)) throw new IllegalArgumentException("Invalid time window");
        Vehicle vehicle = carRentalRepository.findVehicleById(vehicleId).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        vehicle.getVehicleLock().lock();
        try {
            if (hasOverlap(vehicle, start, end)) {
                throw new IllegalStateException("Vehicle is not available for the requested interval");
            }
            Instant now = Instant.now();
            double price = calculatePrice(vehicle.getPricePerHour(), start, end);
            Reservation reservation = new Reservation(UUID.randomUUID().toString(), vehicleId, customerId,
                    start, end, price, now, now.plus(ttl));
            vehicle.getReservationList().add(reservation);
            carRentalRepository.saveReservation(reservation);
            return reservation;
        } finally {
            vehicle.getVehicleLock().unlock();
        }

    }

    @Override
    public void confirmBooking(String reservationId) throws Exception {
        Reservation reservation = carRentalRepository.findReservationById(reservationId).orElseThrow(() -> new Exception("No reservation information found"));

        if( !reservation.getReservationStatus().compareAndSet(ReservationStatus.HELD, ReservationStatus.CONFIRMED) ) {
            throw new Exception("Reservation is no longer in HELD state");
        }
    }

    @Override
    public void cancelBooking(String reservationId) throws Exception {
        Reservation reservation = carRentalRepository.findReservationById(reservationId).orElseThrow(() -> new Exception("No reservation information found"));
        AtomicReference<ReservationStatus> currentReservationStatus = reservation.getReservationStatus();
        ReservationStatus reservationStatus;
        do {
            reservationStatus = currentReservationStatus.get();
            if (reservationStatus.equals(ReservationStatus.CANCELLED) || reservationStatus.equals(ReservationStatus.EXPIRED)) {
                throw new Exception("Reservation cannot be cancelled from current state: " + reservationStatus);
            }
        } while (!currentReservationStatus.compareAndSet(reservationStatus, ReservationStatus.CANCELLED));
    }

    @Override
    public void cleanExpiredReservations() {
        Instant now = Instant.now();
        for (Reservation reservation : carRentalRepository.getAllReservations()) {
            if (reservation.getExpiresAt().isBefore(now)
                    && reservation.getReservationStatus().compareAndSet(ReservationStatus.HELD, ReservationStatus.EXPIRED)) {
                System.out.println(">>> Background Daemon: Expired hold " + reservation.getId()
                        + " on vehicle " + reservation.getVehicleId());
            }
        }
    }
}
