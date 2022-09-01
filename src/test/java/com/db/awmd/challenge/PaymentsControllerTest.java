package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class PaymentsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private NotificationService notificationService;

    @Before
    public void prepare() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        Account account1 = new Account("id1", BigDecimal.ONE);
        Account account2 = new Account("id2", BigDecimal.ONE);

        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(), Mockito.any());
    }

    @After
    public void after() {
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    public void transferMoney() throws Exception {
        Account accountFrom = accountsService.getAccount("id1");
        Account accountTo = accountsService.getAccount("id2");

        this.mockMvc.perform(post("/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"id1\", \"to\": \"id2\", \"amount\":1}")).andExpect(status().isOk());

        assertThat(accountFrom.getBalance()).isEqualByComparingTo("0");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("2");

        Mockito.verify(notificationService, Mockito.times(2)).notifyAboutTransfer(Mockito.any(), Mockito.any());
    }

    @Test
    public void notEnoughMoney() throws Exception {
        this.mockMvc.perform(post("/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"id1\", \"to\": \"id2\", \"amount\":1111}")).andExpect(status().isBadRequest());

        verifyBalanceWasNotChanged();
        verifyNotificationsWasNotSent();
    }

    @Test
    public void accountFromDoesNotExists() throws Exception {
        this.mockMvc.perform(post("/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"fantom\", \"to\": \"id2\", \"amount\":1}")).andExpect(status().isBadRequest());

        verifyBalanceWasNotChanged();
        verifyNotificationsWasNotSent();
    }

    @Test
    public void accountToDoesNotExists() throws Exception {
        this.mockMvc.perform(post("/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"id1\", \"to\": \"fantom\", \"amount\":1}")).andExpect(status().isBadRequest());

        verifyBalanceWasNotChanged();
        verifyNotificationsWasNotSent();
    }

    @Test
    public void requestWithLessThan1Amount() throws Exception {
        this.mockMvc.perform(post("/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"id1\", \"to\": \"id2\", \"amount\":0}")).andExpect(status().isBadRequest());

        verifyBalanceWasNotChanged();
        verifyNotificationsWasNotSent();
    }

    @Test
    public void requestWithEmptyAccountIdFrom() throws Exception {
        this.mockMvc.perform(post("/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"\", \"to\": \"id1\", \"amount\":1}")).andExpect(status().isBadRequest());

        verifyBalanceWasNotChanged();
        verifyNotificationsWasNotSent();
    }

    @Test
    public void requestWithEmptyAccountIdTo() throws Exception {
        this.mockMvc.perform(post("/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"id1\", \"to\": \"\", \"amount\":1}")).andExpect(status().isBadRequest());

        verifyBalanceWasNotChanged();
        verifyNotificationsWasNotSent();
    }

    private void verifyBalanceWasNotChanged() {
        Account accountFrom = accountsService.getAccount("id1");
        Account accountTo = accountsService.getAccount("id2");
        assertThat(accountFrom.getBalance()).isEqualByComparingTo("1");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("1");
    }

    private void verifyNotificationsWasNotSent() {
        Mockito.verify(notificationService, Mockito.times(0)).notifyAboutTransfer(Mockito.any(), Mockito.any());
    }
}
