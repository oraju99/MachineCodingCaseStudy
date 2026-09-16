package org.example.ParkingLot.models;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ParkingLot {
    private final String id;
    private final String name;
    private final List<ParkingSpot> parkingSpots;

    public ParkingLot(String id, String name) {
        this.id = id;
        this.name = name;
        this.parkingSpots = new CopyOnWriteArrayList<>();
    }
}
