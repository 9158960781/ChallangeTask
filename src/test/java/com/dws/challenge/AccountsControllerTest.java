package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.exception.DuplicateAccountIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.mockito.Mock;
import org.mockito.InjectMocks;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Mock
  private AccountsService accountsServiceMock;

  @InjectMocks
  private AccountsController accountsController;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void transferMoney_success() throws Exception {
    Account accountFrom = new Account("Id-123");
    accountFrom.setBalance(new BigDecimal(1000));

    Account accountTo = new Account("Id-456");
    accountTo.setBalance(new BigDecimal(500));

    when(accountsServiceMock.getAccount("Id-123")).thenReturn(accountFrom);
    when(accountsServiceMock.getAccount("Id-456")).thenReturn(accountTo);

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .param("accountFromId", "Id-123")
                    .param("accountToId", "Id-456")
                    .param("amount", "200"))
            .andExpect(status().isOk())
            .andExpect(content().string("Transfer successful"));

    assertThat(accountFrom.getBalance()).isEqualTo(new BigDecimal(800));
    assertThat(accountTo.getBalance()).isEqualTo(new BigDecimal(700));

    // Verifying that the service method was called for transfer
    verify(accountsServiceMock).transferMoney("Id-123", "Id-456", 200.0);
  }

  @Test
  void transferMoney_insufficientFunds() throws Exception {
    Account accountFrom = new Account("Id-123");
    accountFrom.setBalance(new BigDecimal(100));

    Account accountTo = new Account("Id-456");
    accountTo.setBalance(new BigDecimal(500));

    when(accountsServiceMock.getAccount("Id-123")).thenReturn(accountFrom);
    when(accountsServiceMock.getAccount("Id-456")).thenReturn(accountTo);

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .param("accountFromId", "Id-123")
                    .param("accountToId", "Id-456")
                    .param("amount", "200"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Insufficient funds"));

    verify(accountsServiceMock).transferMoney("Id-123", "Id-456", 200.0);
  }

  @Test
  void transferMoney_invalidAccount() throws Exception {
    when(accountsServiceMock.getAccount("Id-123")).thenReturn(null);

    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .param("accountFromId", "Id-123")
                    .param("accountToId", "Id-456")
                    .param("amount", "200"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("One or both accounts do not exist"));
  }
}
