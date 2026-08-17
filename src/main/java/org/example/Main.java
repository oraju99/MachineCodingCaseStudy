package org.example;

import org.example.walletSystem.controller.WalletController;
import org.example.walletSystem.enums.CurrencyEnum;
import org.example.walletSystem.models.Balance;
import org.example.walletSystem.models.Hold;
import org.example.walletSystem.models.LedgerEntry;
import org.example.walletSystem.repository.InMemoryWalletRepository;
import org.example.walletSystem.repository.WalletRepository;
import org.example.walletSystem.service.TransactionService;
import org.example.walletSystem.service.WalletService;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // 1. Dependency Injection Wiring
        WalletRepository repository = new InMemoryWalletRepository();
        TransactionService transactionService = new TransactionService(repository);
        WalletService walletService = new WalletService(repository, transactionService);
        WalletController controller = new WalletController(walletService, transactionService);

        System.out.println("==================================================");
        System.out.println("SCENARIO 1: Onboarding & Initial Balance");
        System.out.println("==================================================");
        controller.createWallet("W_ALICE", "CUST_101", CurrencyEnum.USD);
        controller.createWallet("W_BOB", "CUST_102", CurrencyEnum.USD);

        Balance aliceBal = controller.getBalance("W_ALICE").orElseThrow();
        System.out.println("Alice Initial Balance: Available=" + aliceBal.getAvailableBalance() + ", Locked=" + aliceBal.getLockedBalance());

        System.out.println("\n==================================================");
        System.out.println("SCENARIO 2: Direct Credit & Strict Idempotency Check");
        System.out.println("==================================================");
        BigDecimal creditAmt = new BigDecimal("500.0000");
        String refId1 = "TOPUP_REQ_001";

        // First credit call
        LedgerEntry entry1 = controller.credit("W_ALICE", creditAmt, refId1);
        System.out.println("First Credit Call -> Status: " + entry1.getTransactionStatus() + ", Post Available: " + entry1.getPostAvailableBalance());

        // Repeated credit call with exact same referenceId
        LedgerEntry duplicateEntry = controller.credit("W_ALICE", creditAmt, refId1);
        System.out.println("Duplicate RefId Call -> Status: " + duplicateEntry.getTransactionStatus() + ", Returned Post Available: " + duplicateEntry.getPostAvailableBalance());

        aliceBal = controller.getBalance("W_ALICE").orElseThrow();
        System.out.println("Alice Balance after Duplicate Test: Available=" + aliceBal.getAvailableBalance() + " (Must be 500.0000)");

        System.out.println("\n==================================================");
        System.out.println("SCENARIO 3: Direct Debit & Insufficient Funds Check");
        System.out.println("==================================================");
        // Valid debit
        controller.debit("W_ALICE", new BigDecimal("100.0000"), "PURCHASE_001");
        aliceBal = controller.getBalance("W_ALICE").orElseThrow();
        System.out.println("Alice Balance after $100 Debit: Available=" + aliceBal.getAvailableBalance());

        // Overdraft attempt
        try {
            controller.debit("W_ALICE", new BigDecimal("1000.0000"), "PURCHASE_OVERDRAFT");
        } catch (Exception ex) {
            System.out.println("Overdraft Attempt Correctly Blocked: " + ex.getMessage());
        }

        System.out.println("\n==================================================");
        System.out.println("SCENARIO 4: Two-Phase Reservation & Capture (Success)");
        System.out.println("==================================================");
        // Hold $150
        Hold hold1 = controller.reserveFunds("W_ALICE", new BigDecimal("150.0000"), 5000);
        aliceBal = controller.getBalance("W_ALICE").orElseThrow();
        System.out.println("Hold Placed -> Available=" + aliceBal.getAvailableBalance() + ", Locked=" + aliceBal.getLockedBalance());

        // Capture $150
        controller.captureFunds(hold1.getId());
        aliceBal = controller.getBalance("W_ALICE").orElseThrow();
        System.out.println("Hold Captured -> Available=" + aliceBal.getAvailableBalance() + ", Locked=" + aliceBal.getLockedBalance());

        System.out.println("\n==================================================");
        System.out.println("SCENARIO 5: Two-Phase Reservation & Release (Rollback)");
        System.out.println("==================================================");
        // Hold $50
        Hold hold2 = controller.reserveFunds("W_ALICE", new BigDecimal("50.0000"), 5000);
        aliceBal = controller.getBalance("W_ALICE").orElseThrow();
        System.out.println("Hold Placed -> Available=" + aliceBal.getAvailableBalance() + ", Locked=" + aliceBal.getLockedBalance());

        // Release $50
        controller.releaseFunds(hold2.getId());
        aliceBal = controller.getBalance("W_ALICE").orElseThrow();
        System.out.println("Hold Released -> Available=" + aliceBal.getAvailableBalance() + ", Locked=" + aliceBal.getLockedBalance());

        System.out.println("\n==================================================");
        System.out.println("SCENARIO 6: High-Concurrency Debit Race Condition Test");
        System.out.println("==================================================");
        // Fund Bob with $100
        controller.credit("W_BOB", new BigDecimal("100.0000"), "BOB_FUND_01");

        // 10 concurrent threads each trying to debit $20 simultaneously ($200 total requested against $100 balance)
        int threadsCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch readyLatch = new CountDownLatch(threadsCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadsCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadsCount; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Simultaneous trigger
                    controller.debit("W_BOB", new BigDecimal("20.0000"), "CONCURRENT_DEBIT_" + index);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Fire all threads at once
        doneLatch.await();
        executor.shutdown();

        Balance bobBal = controller.getBalance("W_BOB").orElseThrow();
        System.out.println("Concurrent Debit Test Result:");
        System.out.println("Successful Operations : " + successCount.get() + " (Expected exactly 5)");
        System.out.println("Rejected Operations   : " + failCount.get() + " (Expected exactly 5)");
        System.out.println("Bob Final Available   : " + bobBal.getAvailableBalance() + " (Must be 0.0000)");

        System.out.println("\n==================================================");
        System.out.println("SCENARIO 7: Immutable Audit Trail / Statement for Alice");
        System.out.println("==================================================");
        List<LedgerEntry> statement = controller.getStatement("W_ALICE");
        System.out.printf("%-10s | %-8s | %-12s | %-12s | %-10s%n", "TYPE", "AMOUNT", "PRE_AVAIL", "POST_AVAIL", "STATUS");
        System.out.println("------------------------------------------------------------------");
        for (LedgerEntry entry : statement) {
            System.out.printf("%-10s | %-8s | %-12s | %-12s | %-10s%n",
                    entry.getTransactionType(),
                    entry.getAmount(),
                    entry.getPreAvailableBalance(),
                    entry.getPostAvailableBalance(),
                    entry.getTransactionStatus());
        }
    }
}