package org.example.walletSystem.models;

import lombok.Data;
import org.example.walletSystem.enums.CurrencyEnum;
import org.example.walletSystem.enums.WalletStatus;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class Wallet {
    private final String id;
    private final String customerId;
    private final CurrencyEnum currency;
    private volatile WalletStatus walletStatus;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final ReentrantLock lock = new ReentrantLock(true);

    public Wallet(String id, String customerId, CurrencyEnum currency) {
        Instant currentInstant = Instant.now();
        this.id = id;
        this.customerId = customerId;
        this.currency = currency;
        this.walletStatus = WalletStatus.ACTIVE;
        this.createdAt = currentInstant;
        this.updatedAt = currentInstant;
    }

}
