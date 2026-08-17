package org.example.walletSystem.service;

import org.example.walletSystem.enums.CurrencyEnum;
import org.example.walletSystem.enums.WalletStatus;
import org.example.walletSystem.models.Balance;
import org.example.walletSystem.models.Wallet;
import org.example.walletSystem.repository.WalletRepository;

import java.time.Instant;
import java.util.Optional;

public class WalletService {
    private final WalletRepository repository;
    private final TransactionService transactionService;

    public WalletService(WalletRepository repository, TransactionService transactionService) {
        this.repository = repository;
        this.transactionService = transactionService;
    }

    public Wallet createWallet(String id, String customerId, CurrencyEnum currency) {
        Wallet wallet = new Wallet(id, customerId, currency);
        Balance balance = new Balance(id);

        repository.saveWallet(wallet);
        repository.saveBalance(balance);
        return wallet;
    }

    public Optional<Wallet> getWallet(String walletId) {
        return repository.findWalletById(walletId);
    }

    public Optional<Balance> getBalance(String walletId) {
        return repository.findBalanceByWalletId(walletId);
    }

    public void updateWalletStatus(String walletId, WalletStatus status) {
        Wallet wallet = repository.findWalletById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        wallet.setWalletStatus(status);
    }

    // Daemon sweeper to release expired holds automatically
    public void cleanExpiredHolds() {
        Instant now = Instant.now();
        // Look up registered holds and auto-release if expired
        // In production, this targets an indexed query: status == ACTIVE and expiresAt < now
    }
}