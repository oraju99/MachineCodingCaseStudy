package org.example.CarRentalSystem.repository;

import org.example.CarRentalSystem.models.Branch;
import org.example.CarRentalSystem.models.Reservation;
import org.example.CarRentalSystem.models.Vehicle;

import java.util.List;
import java.util.Optional;

public interface CarRentalRepository {
    void saveBranch(Branch branch);
    Optional<Branch> findBranchById(String id);

    void saveVehicle(Vehicle vehicle);
    Optional<Vehicle> findVehicleById(String id);
    List<Vehicle> getAllVehicles();

    void saveReservation(Reservation reservation);
    Optional<Reservation> findReservationById(String id);
    List<Reservation> getAllReservations();
}
