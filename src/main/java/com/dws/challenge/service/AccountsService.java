package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.NotificationService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transferMoney(String accountFromId, String accountToId, double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Transfer amount must be positive");
    }

    Account accountFrom = this.accountsRepository.getAccount(accountFromId);
    Account accountTo = this.accountsRepository.getAccount(accountToId);

    if (accountFrom == null || accountTo == null) {
      throw new IllegalArgumentException("One or both accounts do not exist");
    }

    // Lock accounts in a consistent order to prevent deadlocks
    Object firstLock = accountFromId.compareTo(accountToId) < 0 ? accountFrom : accountTo;
    Object secondLock = accountFromId.compareTo(accountToId) < 0 ? accountTo : accountFrom;

    synchronized (firstLock) {
      synchronized (secondLock) {
        if (accountFrom.getBalance() < amount) {
          throw new IllegalArgumentException("Insufficient funds in account " + accountFromId);
        }

        // Perform the transfer
        accountFrom.setBalance(accountFrom.getBalance() - amount);
        accountTo.setBalance(accountTo.getBalance() + amount);

        // Update accounts in the repository
        this.accountsRepository.updateAccount(accountFrom);
        this.accountsRepository.updateAccount(accountTo);

        // Notify both account holders
        this.notificationService.notifyAboutTransfer(accountFromId,
                "Transferred " + amount + " to account " + accountToId);
        this.notificationService.notifyAboutTransfer(accountToId,
                "Received " + amount + " from account " + accountFromId);
      }
    }
  }
}
