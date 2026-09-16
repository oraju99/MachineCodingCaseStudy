package org.example.ParkingLot.controller;

import org.example.ParkingLot.models.ParkingLot;
import org.example.ParkingLot.models.ParkingSpot;
import org.example.ParkingLot.models.Ticket;
import org.example.ParkingLot.models.Vehicle;
import org.example.ParkingLot.service.ParkingLotService;

// REST API / Gateway Controller delegating requests down to core service layer
public class ParkingLotController {
    private final ParkingLotService service;

    public ParkingLotController(ParkingLotService service) {
        this.service = service;
    }

    public void addParkingLot(ParkingLot parkingLot) {
        service.addParkingLot(parkingLot);
    }

    public void addParkingSpot(String parkingLotId, ParkingSpot parkingSpot) {
        service.addParkingSpot(parkingLotId, parkingSpot);
    }

    public Ticket parkVehicle(Vehicle vehicle, String parkingLotId) throws Exception {
        return service.parkVehicle(vehicle, parkingLotId);
    }

    public Ticket unparkVehicle(String ticketId) throws Exception {
        return service.unparkVehicle(ticketId);
    }
}
