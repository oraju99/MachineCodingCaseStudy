package org.example.inventoryManagement.controller;

import org.example.inventoryManagement.models.Reservation;
import org.example.inventoryManagement.service.InventoryService;

import java.time.Duration;

// REST API / Gateway Controller delegating requests down to core service layer
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    public void addProduct(String id, String name, double price, int initialStock) {
        service.addProduct(id, name, price, initialStock);
    }

    public void restockProduct(String productId, int quantity) {
        service.restockProduct(productId, quantity);
    }

    public Reservation reserveStock(String userId, String productId, int quantity, Duration ttl) {
        return service.reserveStock(userId, productId, quantity, ttl);
    }

    public void commitReservation(String reservationId) {
        service.commitReservation(reservationId);
    }

    public void cancelReservation(String reservationId) {
        service.cancelReservation(reservationId);
    }
}