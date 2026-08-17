package org.example.walletSystem.controller;

import org.example.walletSystem.enums.CurrencyEnum;
import org.example.walletSystem.enums.WalletStatus;
import org.example.walletSystem.models.Balance;
import org.example.walletSystem.models.Hold;
import org.example.walletSystem.models.LedgerEntry;
import org.example.walletSystem.models.Wallet;
import org.example.walletSystem.service.TransactionService;
import org.example.walletSystem.service.WalletService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Entry router orchestrating client operations across wallet account and transaction domains
public class WalletController {
    private final WalletService walletService;
    private final TransactionService transactionService;

    public WalletController(WalletService walletService, TransactionService transactionService) {
        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    // Account creation and metadata endpoints
    public Wallet createWallet(String id, String customerId, CurrencyEnum currency) {
        return walletService.createWallet(id, customerId, currency);
    }

    public Optional<Wallet> getWallet(String walletId) {
        return walletService.getWallet(walletId);
    }

    public Optional<Balance> getBalance(String walletId) {
        return walletService.getBalance(walletId);
    }

    public void updateWalletStatus(String walletId, WalletStatus status) {
        walletService.updateWalletStatus(walletId, status);
    }

    // Direct financial mutations with idempotency reference keys
    public LedgerEntry credit(String walletId, BigDecimal amount, String referenceId) {
        return transactionService.credit(walletId, amount, referenceId);
    }

    public LedgerEntry debit(String walletId, BigDecimal amount, String referenceId) {
        return transactionService.debit(walletId, amount, referenceId);
    }

    // Two-phase reservation and settlement operations
    public Hold reserveFunds(String walletId, BigDecimal amount, long ttlMillis) {
        return transactionService.reserveFunds(walletId, amount, ttlMillis);
    }

    public LedgerEntry captureFunds(String holdId) {
        return transactionService.captureFunds(holdId);
    }

    public LedgerEntry releaseFunds(String holdId) {
        return transactionService.releaseFunds(holdId);
    }

    // Audit and reconciliation queries
    public List<LedgerEntry> getStatement(String walletId) {
        return transactionService.getStatement(walletId);
    }
}