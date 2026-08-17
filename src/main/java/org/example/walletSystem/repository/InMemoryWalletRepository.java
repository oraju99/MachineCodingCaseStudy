package org.example.walletSystem.repository;

import org.example.walletSystem.models.Balance;
import org.example.walletSystem.models.Hold;
import org.example.walletSystem.models.LedgerEntry;
import org.example.walletSystem.models.Wallet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryWalletRepository implements WalletRepository {
    private final Map<String, Wallet> wallets = new ConcurrentHashMap<>();
    private final Map<String, Balance> balances = new ConcurrentHashMap<>();
    private final Map<String, List<LedgerEntry>> ledgerEntriesByWallet = new ConcurrentHashMap<>();
    private final Map<String, Hold> holds = new ConcurrentHashMap<>();
    private final Map<String, LedgerEntry> idempotencyRecords = new ConcurrentHashMap<>();

    @Override
    public void saveWallet(Wallet wallet) {
        wallets.put(wallet.getId(), wallet);
    }

    @Override
    public Optional<Wallet> findWalletById(String id) {
        return Optional.ofNullable(wallets.get(id));
    }

    @Override
    public void saveBalance(Balance balance) {
        balances.put(balance.getWalletId(), balance);
    }

    @Override
    public Optional<Balance> findBalanceByWalletId(String walletId) {
        return Optional.ofNullable(balances.get(walletId));
    }

    @Override
    public void saveLedgerEntry(LedgerEntry entry) {
        // Appends to thread-safe list preserving chronological ledger order
        ledgerEntriesByWallet
                .computeIfAbsent(entry.getWalletId(), k -> new CopyOnWriteArrayList<>())
                .add(entry);
    }

    @Override
    public List<LedgerEntry> findLedgerEntriesByWalletId(String walletId) {
        List<LedgerEntry> entries = ledgerEntriesByWallet.get(walletId);
        return entries != null ? new ArrayList<>(entries) : Collections.emptyList();
    }

    @Override
    public void saveHold(Hold hold) {
        holds.put(hold.getId(), hold);
    }

    @Override
    public Optional<Hold> findHoldById(String id) {
        return Optional.ofNullable(holds.get(id));
    }

    @Override
    public void saveIdempotencyRecord(String referenceId, LedgerEntry entry) {
        idempotencyRecords.put(referenceId, entry);
    }

    @Override
    public Optional<LedgerEntry> findLedgerEntryByIdempotencyKey(String referenceId) {
        return Optional.ofNullable(idempotencyRecords.get(referenceId));
    }
}
