package org.example.CarRentalSystem.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.CarRentalSystem.enums.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@AllArgsConstructor
@Data
public class Vehicle {
    private final String id;
    private final String branchId;
    private final VehicleType vehicleType;
    private final double pricePerHour;
    private final ReentrantLock vehicleLock = new ReentrantLock(true);
    private final List<Reservation> reservationList = new ArrayList<>();
}
