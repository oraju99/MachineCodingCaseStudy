package org.example.walletSystem.models;

import lombok.Data;
import org.example.walletSystem.enums.HoldStatus;

import java.math.BigDecimal;
import java.time.Instant;

/*
* Active Reservations Table
* Tracks the state of active multiphase payment holds
* (e.g., checkout authorizations, pending withdrawals) before final capture or release
* */
@Data
public class Hold {
    private final String id;
    private final String walletId;
    private final BigDecimal amount;
    private volatile HoldStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;

    public Hold(String id, String walletId, BigDecimal amount, Instant expiresAt) {
        this.id = id;
        this.walletId = walletId;
        this.amount = amount;
        this.status = HoldStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
}