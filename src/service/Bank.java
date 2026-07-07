package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger; // FIX 1: Imported standard Java class

import model.Account;
import model.Transaction;

public class Bank {

    private final Map<Integer, Account> accounts = new ConcurrentHashMap<>();

    // FIX 2: Replaced custom wrapper with Java's AtomicInteger
    private final AtomicInteger idGen = new AtomicInteger(1000);

    public Account createAccount(String ownerName, double initialDeposit, String accountType) {
        // FIX 3: Used incrementAndGet() instead of next()
        int id = this.idGen.incrementAndGet();
        Account acc = new Account(id, ownerName, initialDeposit, accountType);
        this.accounts.put(id, acc);
        return acc;
    }

    public Optional<Account> getAccount(int accountId) {
        return Optional.ofNullable(this.accounts.get(accountId));
    }

    public boolean deposit(int accountId, double amount) {
        Account acc = this.accounts.get(accountId);
        if (acc == null) {
            return false;
        } else {
            acc.deposit(amount);
            return true;
        }
    }

    public boolean withdraw(int accountId, double amount) {
        Account acc = this.accounts.get(accountId);
        return acc != null && acc.withdraw(amount);
    }

    public boolean transfer(int fromId, int toId, double amount) {
        if (fromId == toId) {
            return false;
        } else {
            Account aFrom = this.accounts.get(fromId);
            Account aTo = this.accounts.get(toId);
            if (aFrom != null && aTo != null) {
                // FIX 4: Added 'final' keyword to guarantee safe synchronization
                final Account first = fromId < toId ? aFrom : aTo;
                final Account second = fromId < toId ? aTo : aFrom;
                synchronized(first) {
                    synchronized(second) {
                        if (aFrom.getBalance() < amount) {
                            return false;
                        }

                        aFrom.applyTransferOut(amount, toId);
                        aTo.applyTransferIn(amount, fromId);
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
    }

    public List<Account> listAllAccounts() {
        return new ArrayList<>(this.accounts.values());
    }

    public List<Transaction> getMiniStatement(int accountId, int lastN) {
        Account acc = this.accounts.get(accountId);
        if (acc == null) {
            return Collections.emptyList();
        } else {
            List<Transaction> all = acc.getTransactionHistory();
            if (lastN <= 0) {
                return Collections.emptyList();
            } else {
                int from = Math.max(0, all.size() - lastN);
                return all.subList(from, all.size());
            }
        }
    }

    public void applyInterestToSavings(double annualRatePercent) {
        // FIX 5: Added 'final' keyword to the loop variable
        for(final Account acc : this.accounts.values()) {
            if ("SAVINGS".equalsIgnoreCase(acc.getAccountType())) {
                synchronized(acc) {
                    double monthly = acc.getBalance() * (annualRatePercent / 100.0) / 12.0;
                    if (monthly > 0.0) {
                        acc.deposit(monthly);
                    }
                }
            }
        }
    }

    public boolean deleteAccount(int accountId) {
        return this.accounts.remove(accountId) != null;
    }
}