package org.example.CarRentalSystem.repository;

import org.example.CarRentalSystem.models.Branch;
import org.example.CarRentalSystem.models.Reservation;
import org.example.CarRentalSystem.models.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCarRentalRepository implements CarRentalRepository {
    private final Map<String, Branch> branchMap = new ConcurrentHashMap<>();
    private final Map<String, Vehicle> vehicleMap = new ConcurrentHashMap<>();
    private final Map<String, Reservation> reservationMap = new ConcurrentHashMap<>();

    @Override
    public void saveBranch(Branch branch) {
        branchMap.put(branch.getId(), branch);
    }

    @Override
    public Optional<Branch> findBranchById(String id) {
        return Optional.ofNullable(branchMap.get(id));
    }

    @Override
    public void saveVehicle(Vehicle vehicle) {
        vehicleMap.put(vehicle.getId(), vehicle);
    }

    @Override
    public Optional<Vehicle> findVehicleById(String id) {
        return Optional.ofNullable(vehicleMap.get(id));
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        return new ArrayList<>(vehicleMap.values());
    }

    @Override
    public void saveReservation(Reservation reservation) {
        reservationMap.put(reservation.getId(), reservation);
    }

    @Override
    public Optional<Reservation> findReservationById(String id) {
        return Optional.ofNullable(reservationMap.get(id));
    }

    @Override
    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservationMap.values());
    }
}
