package org.example.ParkingLot.models;

import lombok.Getter;
import org.example.ParkingLot.enums.ParkingSpotStatus;
import org.example.ParkingLot.enums.VehicleType;

import java.util.concurrent.atomic.AtomicReference;

@Getter
public class ParkingSpot {
    private final String id;
    private final VehicleType vehicleType;
    private final AtomicReference<ParkingSpotStatus> parkingSpotStatus;
    private final Integer perHourRate;

    public ParkingSpot(String id, VehicleType vehicleType, Integer perHourRate) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.perHourRate = perHourRate;
        this.parkingSpotStatus = new AtomicReference<>(ParkingSpotStatus.AVAILABLE);
    }
}
