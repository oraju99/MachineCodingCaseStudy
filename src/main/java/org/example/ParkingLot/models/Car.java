package org.example.ParkingLot.models;

import org.example.ParkingLot.enums.VehicleType;

public class Car extends Vehicle {
    public Car(String number) {
        super(number, VehicleType.CAR);
    }
}
