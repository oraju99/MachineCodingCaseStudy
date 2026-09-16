package org.example.ParkingLot.models;

import lombok.Getter;
import org.example.ParkingLot.enums.TicketStatus;
import org.example.ParkingLot.enums.VehicleType;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class Ticket {
    private final String id;
    private final VehicleType vehicleType;
    private final String parkingLotId;
    private final String parkingSpotId;
    private final Instant arrivalTime;
    private final Integer perHourRate;
    private final AtomicReference<TicketStatus> status;
    private Instant exitTime;
    private Integer charges;

    public Ticket(String id, VehicleType vehicleType, String parkingLotId, String parkingSpotId, Instant arrivalTime, Integer perHourRate) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.parkingLotId = parkingLotId;
        this.parkingSpotId = parkingSpotId;
        this.arrivalTime = arrivalTime;
        this.perHourRate = perHourRate;
        this.status = new AtomicReference<>(TicketStatus.OPEN);
        this.exitTime = null;
        this.charges = null;
    }

    public void completeCheckout(Instant exitTime, Integer charges) {
        // CAS is the actual guard against a double checkout race — the exitTime/charges writes
        // below only ever run in the single thread that wins this transition
        if (!status.compareAndSet(TicketStatus.OPEN, TicketStatus.CLOSED)) {
            throw new IllegalStateException("Ticket " + id + " has already been checked out");
        }
        this.exitTime = exitTime;
        this.charges = charges;
    }
}
