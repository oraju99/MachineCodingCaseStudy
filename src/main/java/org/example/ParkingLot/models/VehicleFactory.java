package org.example.ParkingLot.models;

import org.example.ParkingLot.enums.VehicleType;

public class VehicleFactory {
    public Vehicle getVehicle(String vehicleNumber, VehicleType vehicleType) {
        return switch (vehicleType) {
            case CAR -> new Car(vehicleNumber);
            case BIKE -> new Bike(vehicleNumber);
            case SUV -> new Suv(vehicleNumber);
        };
    }
}
