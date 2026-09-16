package org.example.ParkingLot.models;

import org.example.ParkingLot.enums.VehicleType;

public class Suv extends Vehicle {
    public Suv(String number) {
        super(number, VehicleType.SUV);
    }
}
