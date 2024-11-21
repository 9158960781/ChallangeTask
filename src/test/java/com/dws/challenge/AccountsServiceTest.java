package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import com.dws.challenge.repository.AccountsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Mock
  private AccountsRepository accountsRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private AccountsService accountsServiceWithMocks;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  // Test for successful transfer of money between accounts
  @Test
  void transferMoney_success() {
    Account accountFrom = new Account("Id-123");
    accountFrom.setBalance(new BigDecimal(1000));

    Account accountTo = new Account("Id-456");
    accountTo.setBalance(new BigDecimal(500));

    when(accountsRepository.getAccount("Id-123")).thenReturn(accountFrom);
    when(accountsRepository.getAccount("Id-456")).thenReturn(accountTo);

    // Inject the mock NotificationService
    accountsServiceWithMocks.transferMoney("Id-123", "Id-456", 200.0);

    assertThat(accountFrom.getBalance()).isEqualTo(new BigDecimal(800)); // 1000 - 200
    assertThat(accountTo.getBalance()).isEqualTo(new BigDecimal(700)); // 500 + 200

    verify(notificationService).notifyAboutTransfer("Id-123", "Transferred 200.0 to account Id-456");
    verify(notificationService).notifyAboutTransfer("Id-456", "Received 200.0 from account Id-123");
  }

  // Test for transfer failure when there are insufficient funds
  @Test
  void transferMoney_insufficientFunds() {
    Account accountFrom = new Account("Id-123");
    accountFrom.setBalance(new BigDecimal(100));

    Account accountTo = new Account("Id-456");
    accountTo.setBalance(new BigDecimal(500));

    when(accountsRepository.getAccount("Id-123")).thenReturn(accountFrom);
    when(accountsRepository.getAccount("Id-456")).thenReturn(accountTo);

    try {
      accountsServiceWithMocks.transferMoney("Id-123", "Id-456", 200.0);
      fail("Should have thrown an exception due to insufficient funds");
    } catch (IllegalArgumentException ex) {
      assertThat(ex.getMessage()).isEqualTo("Insufficient funds in account Id-123");
    }
  }

  // Test for transfer failure when account doesn't exist
  @Test
  void transferMoney_accountNotFound() {
    when(accountsRepository.getAccount("Id-123")).thenReturn(null);

    try {
      accountsServiceWithMocks.transferMoney("Id-123", "Id-456", 200.0);
      fail("Should have thrown an exception when account is not found");
    } catch (IllegalArgumentException ex) {
      assertThat(ex.getMessage()).isEqualTo("One or both accounts do not exist");
    }
  }

  // Test for invalid transfer amount (<= 0)
  @Test
  void transferMoney_invalidAmount() {
    Account accountFrom = new Account("Id-123");
    accountFrom.setBalance(new BigDecimal(1000));

    Account accountTo = new Account("Id-456");
    accountTo.setBalance(new BigDecimal(500));

    when(accountsRepository.getAccount("Id-123")).thenReturn(accountFrom);
    when(accountsRepository.getAccount("Id-456")).thenReturn(accountTo);

    try {
      accountsServiceWithMocks.transferMoney("Id-123", "Id-456", -100.0);
      fail("Should have thrown an exception for negative amount");
    } catch (IllegalArgumentException ex) {
      assertThat(ex.getMessage()).isEqualTo("Transfer amount must be positive");
    }
  }
}
