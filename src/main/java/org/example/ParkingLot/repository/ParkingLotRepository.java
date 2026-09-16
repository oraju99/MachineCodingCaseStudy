package org.example.ParkingLot.repository;

import org.example.ParkingLot.models.ParkingLot;
import org.example.ParkingLot.models.ParkingSpot;
import org.example.ParkingLot.models.Ticket;

import java.util.List;
import java.util.Optional;

public interface ParkingLotRepository {
    void saveParkingLot(ParkingLot parkingLot);
    Optional<ParkingLot> getParkingLotById(String id);
    List<ParkingSpot> getParkingSpotsForParkingLot(String parkingLotId);

    void addParkingSpot(String parkingLotId, ParkingSpot parkingSpot);
    Optional<ParkingSpot> findParkingSpotById(String parkingSpotId);

    void saveTicket(Ticket ticket);
    Optional<Ticket> getTicketById(String id);
}
