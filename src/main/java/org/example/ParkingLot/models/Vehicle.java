package org.example.ParkingLot.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.ParkingLot.enums.VehicleType;

@AllArgsConstructor
@Getter
public abstract class Vehicle {
    private final String number;
    private final VehicleType vehicleType;
}
