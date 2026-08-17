package org.example.inventoryManagement.repository;

import org.example.inventoryManagement.models.Product;
import org.example.inventoryManagement.models.Reservation;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    void saveProduct(Product product);
    Optional<Product> findProductById(String id);
    void saveReservation(Reservation reservation);
    Optional<Reservation> findReservationById(String id);
    List<Reservation> getAllReservations();
}