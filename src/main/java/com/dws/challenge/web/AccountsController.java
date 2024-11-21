package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
      this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  /**
   * Endpoint to handle money transfer between accounts.
   *
   * @param accountFromId ID of the account to transfer money from
   * @param accountToId   ID of the account to transfer money to
   * @param amount        Amount of money to transfer
   * @return Response indicating success or failure of the transfer
   */
  @PostMapping(path = "/transfer")
  public ResponseEntity<String> transferMoney(@RequestParam String accountFromId,
                                              @RequestParam String accountToId,
                                              @RequestParam double amount) {
    log.info("Transferring {} from account {} to account {}", amount, accountFromId, accountToId);

    try {
      this.accountsService.transferMoney(accountFromId, accountToId, amount);
    } catch (IllegalArgumentException ex) {
      log.error("Error during transfer: {}", ex.getMessage());
      return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>("Transfer successful", HttpStatus.OK);
  }
}
