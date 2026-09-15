package org.example.CarRentalSystem.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.CarRentalSystem.enums.VehicleType;

@Getter
@AllArgsConstructor
public class VehicleAvailability {
    private final String vehicleId;
    private final String branchId;
    private final VehicleType vehicleType;
    private final double pricePerHour;
    private final double estimatedTotalPrice;
}
