package org.example.ParkingLot.service;

import org.example.ParkingLot.enums.ParkingSpotStatus;
import org.example.ParkingLot.models.ParkingLot;
import org.example.ParkingLot.models.ParkingSpot;
import org.example.ParkingLot.models.Ticket;
import org.example.ParkingLot.models.Vehicle;
import org.example.ParkingLot.repository.InMemoryParkingLotRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParkingLotService {

    private final InMemoryParkingLotRepository parkingLotRepository;

    public ParkingLotService(InMemoryParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    public void addParkingLot(ParkingLot parkingLot) {
        parkingLotRepository.saveParkingLot(parkingLot);
    }

    public void addParkingSpot(String parkingLotId, ParkingSpot parkingSpot) {
        parkingLotRepository.addParkingSpot(parkingLotId, parkingSpot);
    }

    public void unreserveParkingSpot(String parkingSpotId) throws Exception {
        ParkingSpot parkingSpot = parkingLotRepository.findParkingSpotById(parkingSpotId)
                .orElseThrow(() -> new Exception("No parking spot found for this parking spot id"));

        if( !parkingSpot.getParkingSpotStatus().compareAndSet(ParkingSpotStatus.OCCUPIED, ParkingSpotStatus.AVAILABLE) ) {
            throw new Exception("Parking spot already available");
        }
    }

    public ParkingSpot findAndReserveParkingSpot(Vehicle vehicle, String parkingLotId) throws Exception {
        List<ParkingSpot> parkingSpotList = parkingLotRepository.getParkingSpotsForParkingLot(parkingLotId);
        for( ParkingSpot parkingSpot : parkingSpotList ) {
            // CAS is the only check needed here — a separate get() first would leave a gap another thread can win in between
            if( parkingSpot.getVehicleType() == vehicle.getVehicleType()
                    && parkingSpot.getParkingSpotStatus().compareAndSet(ParkingSpotStatus.AVAILABLE, ParkingSpotStatus.OCCUPIED) ) {
                return parkingSpot;
            }
        }
        throw new Exception("No vacant parking spot for vehicle type: " + vehicle.getVehicleType());
    }

    public Ticket parkVehicle(Vehicle vehicle, String parkingLotId) throws Exception {
        ParkingSpot parkingSpot = findAndReserveParkingSpot(vehicle, parkingLotId);
        Ticket ticket = new Ticket(UUID.randomUUID().toString(), vehicle.getVehicleType(), parkingLotId, parkingSpot.getId(), Instant.now(), parkingSpot.getPerHourRate());

        parkingLotRepository.saveTicket(ticket);
        return ticket;
    }

    public Ticket unparkVehicle(String ticketId) throws Exception {
        Ticket ticket = parkingLotRepository.getTicketById(ticketId).orElseThrow(() -> new Exception("No ticket found for the id"));
        Instant currentInstant = Instant.now();

        int charges = calculateCharges(ticket.getArrivalTime(), currentInstant, ticket.getPerHourRate());
        ticket.completeCheckout(currentInstant, charges);
        unreserveParkingSpot(ticket.getParkingSpotId());

        return ticket;
    }

    private int calculateCharges(Instant arrival, Instant exit, int perHourRate) {
        long seconds = Duration.between(arrival, exit).getSeconds();
        long billableHours = (seconds + 3599) / 3600;
        return (int) (billableHours * perHourRate);
    }
}
