package org.example.walletSystem.models;

import lombok.Data;
import org.example.walletSystem.enums.TransactionType;
import org.example.walletSystem.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/*
* Transaction Ledger Table
* An append-only, immutable record of all financial events.
* This table serves as the single source of truth for audits, reconciliation, and balance derivation.
* */
@Data
public class LedgerEntry {
    private final String id;
    private final String walletId;
    private final String referenceId;
    private final TransactionType transactionType;
    private final TransactionStatus transactionStatus;
    private final BigDecimal amount;
    private final BigDecimal preAvailableBalance;
    private final BigDecimal postAvailableBalance;
    private final BigDecimal preLockedBalance;
    private final BigDecimal postLockedBalance;
    private final Instant createdAt;

    public LedgerEntry(String id, String walletId, String referenceId, TransactionType type,
                       BigDecimal amount, TransactionStatus status,
                       BigDecimal preAvailableBalance, BigDecimal postAvailableBalance,
                       BigDecimal preLockedBalance, BigDecimal postLockedBalance) {
        this.id = id;
        this.walletId = walletId;
        this.referenceId = referenceId;
        this.transactionType = type;
        this.amount = amount;
        this.transactionStatus = status;
        this.preAvailableBalance = preAvailableBalance;
        this.postAvailableBalance = postAvailableBalance;
        this.preLockedBalance = preLockedBalance;
        this.postLockedBalance = postLockedBalance;
        this.createdAt = Instant.now();
    }
}
