package org.example.walletSystem.service;

import org.example.walletSystem.enums.HoldStatus;
import org.example.walletSystem.enums.TransactionStatus;
import org.example.walletSystem.enums.TransactionType;
import org.example.walletSystem.enums.WalletStatus;
import org.example.walletSystem.models.Balance;
import org.example.walletSystem.models.Hold;
import org.example.walletSystem.models.LedgerEntry;
import org.example.walletSystem.models.Wallet;
import org.example.walletSystem.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionService {
    private final WalletRepository repository;

    public TransactionService(WalletRepository walletRepository) {
        this.repository = walletRepository;
    }

    // Direct atomic credit with idempotency support
    public LedgerEntry credit(String walletId, BigDecimal amount, String referenceId) {
        validatePositiveAmount(amount);

        Optional<LedgerEntry> ledgerEntry = repository.findLedgerEntryByIdempotencyKey(referenceId);
        if (ledgerEntry.isPresent()) {
            return ledgerEntry.get();
        }

        Wallet activeWallet = getActiveWallet(walletId);
        ReentrantLock reentrantLock = activeWallet.getLock();
        reentrantLock.lock();
        try {
            ledgerEntry = repository.findLedgerEntryByIdempotencyKey(referenceId);
            if (ledgerEntry.isPresent()) {
                return ledgerEntry.get();
            }

            Balance balance = getBalance(walletId);

            BigDecimal preAvailable = balance.getAvailableBalance();
            BigDecimal preLocked = balance.getLockedBalance();
            BigDecimal postAvailable = preAvailable.add(amount);

            balance.setAvailableBalance(postAvailable);
            balance.setVersion(balance.getVersion() + 1);
            balance.setUpdatedAt(Instant.now());

            LedgerEntry entry = new LedgerEntry(
                    UUID.randomUUID().toString(), walletId, referenceId,
                    TransactionType.CREDIT, amount, TransactionStatus.COMPLETED,
                    preAvailable, postAvailable, preLocked, preLocked
            );

            repository.saveLedgerEntry(entry);
            repository.saveIdempotencyRecord(referenceId, entry);
            return entry;
        } finally {
            reentrantLock.unlock();
        }
    }

    // Direct atomic debit with balance constraint validation
    public LedgerEntry debit(String walletId, BigDecimal amount, String referenceId) {
        validatePositiveAmount(amount);

        Optional<LedgerEntry> ledgerEntry = repository.findLedgerEntryByIdempotencyKey(referenceId);
        if (ledgerEntry.isPresent()) {
            return ledgerEntry.get();
        }

        Wallet activeWallet = getActiveWallet(walletId);
        ReentrantLock reentrantLock = activeWallet.getLock();
        reentrantLock.lock();
        try {
            ledgerEntry = repository.findLedgerEntryByIdempotencyKey(referenceId);
            if (ledgerEntry.isPresent()) {
                return ledgerEntry.get();
            }

            Balance balance = getBalance(walletId);
            if (balance.getAvailableBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds in wallet: " + walletId);
            }

            BigDecimal preAvailable = balance.getAvailableBalance();
            BigDecimal preLocked = balance.getLockedBalance();
            BigDecimal postAvailable = preAvailable.subtract(amount);

            balance.setAvailableBalance(postAvailable);
            balance.setVersion(balance.getVersion() + 1);
            balance.setUpdatedAt(Instant.now());

            LedgerEntry entry = new LedgerEntry(
                    UUID.randomUUID().toString(), walletId, referenceId,
                    TransactionType.DEBIT, amount, TransactionStatus.COMPLETED,
                    preAvailable, postAvailable, preLocked, preLocked
            );

            repository.saveLedgerEntry(entry);
            repository.saveIdempotencyRecord(referenceId, entry);
            return entry;

        } finally {
            reentrantLock.unlock();
        }
    }

    // Two-Phase Hold: moves funds from Available to Locked
    public Hold reserveFunds(String walletId, BigDecimal amount, long ttlMillis) {
        validatePositiveAmount(amount);

        Wallet activeWallet = getActiveWallet(walletId);
        ReentrantLock reentrantLock = activeWallet.getLock();
        reentrantLock.lock();

        try {
            Balance balance = getBalance(walletId);
            if (balance.getAvailableBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds to place hold");
            }

            BigDecimal preAvailable = balance.getAvailableBalance();
            BigDecimal preLocked = balance.getLockedBalance();
            BigDecimal postAvailable = preAvailable.subtract(amount);
            BigDecimal postLocked = preLocked.add(amount);

            balance.setAvailableBalance(postAvailable);
            balance.setLockedBalance(postLocked);
            balance.setVersion(balance.getVersion() + 1);
            balance.setUpdatedAt(Instant.now());

            Hold hold = new Hold(UUID.randomUUID().toString(), walletId, amount, Instant.now().plusMillis(ttlMillis));
            repository.saveHold(hold);

            LedgerEntry entry = new LedgerEntry(
                    UUID.randomUUID().toString(), walletId, hold.getId(),
                    TransactionType.RESERVE, amount, TransactionStatus.PENDING,
                    preAvailable, postAvailable, preLocked, postLocked
            );
            repository.saveLedgerEntry(entry);

            return hold;
        } finally {
            reentrantLock.unlock();
        }
    }

    // Capture Hold: permanently deducts locked funds
    public LedgerEntry captureFunds(String holdId) {
        Hold hold = repository.findHoldById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));

        Wallet wallet = getActiveWallet(hold.getWalletId());
        ReentrantLock reentrantLock = wallet.getLock();
        reentrantLock.lock();
        try {
            if (hold.getStatus() != HoldStatus.ACTIVE) {
                throw new IllegalStateException("Hold is not active: " + hold.getStatus());
            }

            Balance balance = getBalance(hold.getWalletId());
            BigDecimal preAvailable = balance.getAvailableBalance();
            BigDecimal preLocked = balance.getLockedBalance();
            BigDecimal postLocked = preLocked.subtract(hold.getAmount());

            balance.setLockedBalance(postLocked);
            balance.setVersion(balance.getVersion() + 1);
            balance.setUpdatedAt(Instant.now());

            hold.setStatus(HoldStatus.CAPTURED);

            LedgerEntry entry = new LedgerEntry(
                    UUID.randomUUID().toString(), hold.getWalletId(), hold.getId(),
                    TransactionType.CAPTURE, hold.getAmount(), TransactionStatus.COMPLETED,
                    preAvailable, preAvailable, preLocked, postLocked
            );
            repository.saveLedgerEntry(entry);
            return entry;

        } finally {
            reentrantLock.unlock();
        }

    }

    // Release Hold: reverts locked funds back to available balance
    public LedgerEntry releaseFunds(String holdId) {
        Hold hold = repository.findHoldById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));

        Wallet wallet = getActiveWallet(hold.getWalletId());
        ReentrantLock lock = wallet.getLock();
        lock.lock();
        try {
            if (hold.getStatus() != HoldStatus.ACTIVE) {
                throw new IllegalStateException("Hold is not active: " + hold.getStatus());
            }

            Balance balance = getBalance(hold.getWalletId());
            BigDecimal preAvailable = balance.getAvailableBalance();
            BigDecimal preLocked = balance.getLockedBalance();
            BigDecimal postAvailable = preAvailable.add(hold.getAmount());
            BigDecimal postLocked = preLocked.subtract(hold.getAmount());

            balance.setAvailableBalance(postAvailable);
            balance.setLockedBalance(postLocked);
            balance.setVersion(balance.getVersion() + 1);
            balance.setUpdatedAt(Instant.now());

            hold.setStatus(HoldStatus.RELEASED);

            LedgerEntry entry = new LedgerEntry(
                    UUID.randomUUID().toString(), hold.getWalletId(), hold.getId(),
                    TransactionType.RELEASE, hold.getAmount(), TransactionStatus.REVERSED,
                    preAvailable, postAvailable, preLocked, postLocked
            );
            repository.saveLedgerEntry(entry);
            return entry;
        } finally {
            lock.unlock();
        }
    }

    public List<LedgerEntry> getStatement(String walletId) {
        return repository.findLedgerEntriesByWalletId(walletId);
    }

    private Wallet getActiveWallet(String walletId) {
        Wallet wallet = repository.findWalletById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        if (wallet.getWalletStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active: " + wallet.getWalletStatus());
        }
        return wallet;
    }

    private Balance getBalance(String walletId) {
        return repository.findBalanceByWalletId(walletId)
                .orElseThrow(() -> new IllegalStateException("Balance projection missing for wallet: " + walletId));
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
    }


}
