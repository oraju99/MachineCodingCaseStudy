package org.example.ParkingLot.repository;

import org.example.ParkingLot.models.ParkingLot;
import org.example.ParkingLot.models.ParkingSpot;
import org.example.ParkingLot.models.Ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryParkingLotRepository implements ParkingLotRepository {
    Map<String, ParkingLot> parkingLotMap = new ConcurrentHashMap<>();
    Map<String, ParkingSpot> parkingSpotMap = new ConcurrentHashMap<>();
    Map<String, Ticket> ticketMap = new ConcurrentHashMap<>();

    @Override
    public void saveParkingLot(ParkingLot parkingLot) {
        parkingLotMap.put(parkingLot.getId(), parkingLot);
    }

    @Override
    public Optional<ParkingLot> getParkingLotById(String id) {
        return Optional.ofNullable(parkingLotMap.get(id));
    }

    @Override
    public List<ParkingSpot> getParkingSpotsForParkingLot(String parkingLotId) {
        ParkingLot parkingLot = getParkingLotById(parkingLotId)
                .orElseThrow(() -> new IllegalArgumentException("Parking lot not found: " + parkingLotId));
        return new ArrayList<>(parkingLot.getParkingSpots());
    }

    @Override
    public void addParkingSpot(String parkingLotId, ParkingSpot parkingSpot) {
        ParkingLot parkingLot = getParkingLotById(parkingLotId)
                .orElseThrow(() -> new IllegalArgumentException("Parking lot not found: " + parkingLotId));
        parkingLot.getParkingSpots().add(parkingSpot);
        parkingSpotMap.put(parkingSpot.getId(), parkingSpot);
    }

    @Override
    public Optional<ParkingSpot> findParkingSpotById(String parkingSpotId) {
        return Optional.ofNullable(parkingSpotMap.get(parkingSpotId));
    }

    @Override
    public void saveTicket(Ticket ticket) {
        ticketMap.put(ticket.getId(), ticket);
    }

    @Override
    public Optional<Ticket> getTicketById(String id) {
        return Optional.ofNullable(ticketMap.get(id));
    }


}
