package org.example.ParkingLot.models;

import org.example.ParkingLot.enums.VehicleType;

public class Bike extends Vehicle {
    public Bike(String number) {
        super(number, VehicleType.BIKE);
    }
}
