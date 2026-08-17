package org.example.inventoryManagement.repository;

import org.example.inventoryManagement.models.Product;
import org.example.inventoryManagement.models.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Lock-free concurrently accessible repository implementation
public class InMemoryInventoryRepository implements InventoryRepository {
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public void saveProduct(Product product) {
        products.put(product.getId(), product);
    }

    @Override
    public Optional<Product> findProductById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public void saveReservation(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
    }

    @Override
    public Optional<Reservation> findReservationById(String id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }
}