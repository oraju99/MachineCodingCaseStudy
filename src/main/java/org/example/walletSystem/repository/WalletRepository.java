package org.example.walletSystem.repository;

import org.example.walletSystem.models.Balance;
import org.example.walletSystem.models.Hold;
import org.example.walletSystem.models.LedgerEntry;
import org.example.walletSystem.models.Wallet;

import java.util.List;
import java.util.Optional;

public interface WalletRepository {
    void saveWallet(Wallet wallet);
    Optional<Wallet> findWalletById(String id);

    void saveBalance(Balance balance);
    Optional<Balance> findBalanceByWalletId(String walletId);

    void saveLedgerEntry(LedgerEntry entry);
    List<LedgerEntry> findLedgerEntriesByWalletId(String walletId);

    void saveHold(Hold hold);
    Optional<Hold> findHoldById(String id);

    void saveIdempotencyRecord(String referenceId, LedgerEntry entry);
    Optional<LedgerEntry> findLedgerEntryByIdempotencyKey(String referenceId);
}