package org.example.walletSystem.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/*
* Balance Projection Table
* Maintains the latest derived balance state for high-speed reads and atomic validation.
* This acts as a materialized projection derived from the immutable ledger.
* */
@Data
public class Balance {
    private final String walletId;
    private BigDecimal availableBalance; // Spendable funds available for transactions
    private BigDecimal lockedBalance;    // Funds held for active reservations
    private long version;                // Optimistic concurrency control tracker
    private Instant updatedAt;

    public Balance(String walletId) {
        this.walletId = walletId;
        this.availableBalance = BigDecimal.ZERO.setScale(4);
        this.lockedBalance = BigDecimal.ZERO.setScale(4);
        this.version = 0L;
        this.updatedAt = Instant.now();
    }
}
