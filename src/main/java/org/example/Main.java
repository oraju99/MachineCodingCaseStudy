package org.example;

import org.example.ParkingLot.controller.ParkingLotController;
import org.example.ParkingLot.enums.VehicleType;
import org.example.ParkingLot.models.ParkingLot;
import org.example.ParkingLot.models.ParkingSpot;
import org.example.ParkingLot.models.Ticket;
import org.example.ParkingLot.models.Vehicle;
import org.example.ParkingLot.models.VehicleFactory;
import org.example.ParkingLot.repository.InMemoryParkingLotRepository;
import org.example.ParkingLot.service.ParkingLotService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        InMemoryParkingLotRepository repo = new InMemoryParkingLotRepository();
        ParkingLotService service = new ParkingLotService(repo);
        ParkingLotController controller = new ParkingLotController(service);
        VehicleFactory vehicleFactory = new VehicleFactory();

        controller.addParkingLot(new ParkingLot("LOT1", "Downtown Lot"));
        controller.addParkingSpot("LOT1", new ParkingSpot("S1", VehicleType.CAR, 20));
        controller.addParkingSpot("LOT1", new ParkingSpot("S2", VehicleType.CAR, 20));
        controller.addParkingSpot("LOT1", new ParkingSpot("S3", VehicleType.BIKE, 10));
        controller.addParkingSpot("LOT1", new ParkingSpot("S4", VehicleType.SUV, 30));

        System.out.println("--- Scenario 1: Park Vehicles Into Distinct Available Spots ---");
        Vehicle car1 = vehicleFactory.getVehicle("KA-01-1111", VehicleType.CAR);
        Vehicle car2 = vehicleFactory.getVehicle("KA-01-2222", VehicleType.CAR);
        Ticket car1Ticket = null;
        try {
            car1Ticket = controller.parkVehicle(car1, "LOT1");
            System.out.println("SUCCESS: Parked " + car1.getNumber() + " -> Spot " + car1Ticket.getParkingSpotId() + ", Ticket " + car1Ticket.getId());
        } catch (Exception e) {
            System.out.println("REJECTED: " + car1.getNumber() + " - " + e.getMessage());
        }
        try {
            Ticket t = controller.parkVehicle(car2, "LOT1");
            System.out.println("SUCCESS: Parked " + car2.getNumber() + " -> Spot " + t.getParkingSpotId() + ", Ticket " + t.getId());
        } catch (Exception e) {
            System.out.println("REJECTED: " + car2.getNumber() + " - " + e.getMessage());
        }

        System.out.println();
        System.out.println("--- Scenario 2: Reject Parking When No Matching Spot Is Vacant ---");
        Vehicle car3 = vehicleFactory.getVehicle("KA-01-3333", VehicleType.CAR);
        try {
            Ticket t = controller.parkVehicle(car3, "LOT1");
            System.out.println("SUCCESS: Parked " + car3.getNumber() + " -> Spot " + t.getParkingSpotId());
        } catch (Exception e) {
            System.out.println("REJECTED: " + car3.getNumber() + " - " + e.getMessage() + " (expected: both CAR spots full)");
        }

        Vehicle bike1 = vehicleFactory.getVehicle("KA-01-4444", VehicleType.BIKE);
        Vehicle bike2 = vehicleFactory.getVehicle("KA-01-5555", VehicleType.BIKE);
        try {
            Ticket t = controller.parkVehicle(bike1, "LOT1");
            System.out.println("SUCCESS: Parked " + bike1.getNumber() + " -> Spot " + t.getParkingSpotId());
        } catch (Exception e) {
            System.out.println("REJECTED: " + bike1.getNumber() + " - " + e.getMessage());
        }
        try {
            Ticket t = controller.parkVehicle(bike2, "LOT1");
            System.out.println("SUCCESS: Parked " + bike2.getNumber() + " -> Spot " + t.getParkingSpotId());
        } catch (Exception e) {
            System.out.println("REJECTED: " + bike2.getNumber() + " - " + e.getMessage() + " (expected: only 1 BIKE spot)");
        }

        System.out.println();
        System.out.println("--- Scenario 3: Unpark Releases The Spot For Reuse ---");
        try {
            Ticket closed = controller.unparkVehicle(car1Ticket.getId());
            System.out.println("SUCCESS: Unparked " + car1.getNumber() + " -> Charges: " + closed.getCharges() + " (exit: " + closed.getExitTime() + ")");
        } catch (Exception e) {
            System.out.println("REJECTED: unpark " + car1Ticket.getId() + " - " + e.getMessage());
        }
        try {
            Ticket t = controller.parkVehicle(car3, "LOT1");
            System.out.println("SUCCESS: Parked " + car3.getNumber() + " -> Spot " + t.getParkingSpotId() + " (expected: reuses the freed spot)");
        } catch (Exception e) {
            System.out.println("REJECTED: " + car3.getNumber() + " - " + e.getMessage());
        }
        try {
            controller.unparkVehicle(car1Ticket.getId());
            System.out.println("SUCCESS: double-unpark of " + car1Ticket.getId() + " (unexpected!)");
        } catch (Exception e) {
            System.out.println("REJECTED: double-unpark of " + car1Ticket.getId() + " - " + e.getMessage() + " (expected: already checked out)");
        }

        System.out.println();
        System.out.println("--- Scenario 4: Concurrent Race - 8 Threads Competing For 2 SUV Spots ---");
        controller.addParkingLot(new ParkingLot("LOT2", "Airport Lot"));
        controller.addParkingSpot("LOT2", new ParkingSpot("S5", VehicleType.SUV, 30));
        controller.addParkingSpot("LOT2", new ParkingSpot("S6", VehicleType.SUV, 30));

        int numThreads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        Set<String> claimedSpotIds = ConcurrentHashMap.newKeySet();

        for (int i = 1; i <= numThreads; i++) {
            Vehicle suv = vehicleFactory.getVehicle("KA-01-SUV-" + i, VehicleType.SUV);
            executor.submit(() -> {
                try {
                    latch.await();
                    Ticket t = controller.parkVehicle(suv, "LOT2");
                    claimedSpotIds.add(t.getParkingSpotId());
                    successCount.incrementAndGet();
                    System.out.println("SUCCESS: " + suv.getNumber() + " parked -> Spot " + t.getParkingSpotId());
                } catch (Exception e) {
                    System.out.println("REJECTED: " + suv.getNumber() + " - " + e.getMessage());
                }
            });
        }
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println();
        System.out.println("Total successful parks: " + successCount.get() + " (expected exactly 2)");
        System.out.println("Distinct spots claimed: " + claimedSpotIds.size() + " (expected exactly 2, proves no double-booking)");
    }
}
