package org.example.inventoryManagement.models;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Product {
    private final String id;
    private final String name;
    private final double price;
    private final AtomicInteger availableStock; // Atomic counter for quick, non-blocking stock checks
    private final AtomicInteger reservedStock;  // Tracks currently held stock in pending checkouts
    private final ReentrantLock productLock = new ReentrantLock(true); // Fair lock for atomic multi-step reservation allocations

    public Product(String id, String name, double price, int initialStock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.availableStock = new AtomicInteger(initialStock);
        this.reservedStock = new AtomicInteger(0);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public AtomicInteger getAvailableStock() { return availableStock; }
    public AtomicInteger getReservedStock() { return reservedStock; }
    public ReentrantLock getProductLock() { return productLock; }
}