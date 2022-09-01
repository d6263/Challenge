package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Payment;
import com.db.awmd.challenge.exception.AccountDoesNotExistException;
import com.db.awmd.challenge.exception.NotEnoughBalanceException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentsService {

    private final AccountsRepository accountsRepository;
    private final NotificationService notificationService;

    public void doPayment(Payment payment) {
        Account accountFrom = accountsRepository.getAccount(payment.getFrom());
        Account accountTo = accountsRepository.getAccount(payment.getTo());

        if (Objects.isNull(accountFrom) || Objects.isNull(accountTo)) {
            throw new AccountDoesNotExistException("At least one account doesn't exist, ids: " +
                    payment.getFrom() + ", " + payment.getTo());
        }

        synchronized (this) {
            BigDecimal accountFromBalanceAfterTransaction = accountFrom.getBalance().subtract(payment.getAmount());

            if (accountFromBalanceAfterTransaction.compareTo(BigDecimal.ZERO) < 0) {
                throw new NotEnoughBalanceException("Account " + payment.getFrom() +
                        " has not enough balance for this transaction, bal: " +
                        accountFrom.getBalance() + ", payment amount: " + payment.getAmount());
            }

            accountFrom.setBalance(accountFromBalanceAfterTransaction);
            accountTo.setBalance(accountTo.getBalance().add(payment.getAmount()));
        }

        notificationService.notifyAboutTransfer(
                accountTo, String.format("Received %s from %s",
                        payment.getAmount(), payment.getFrom()));

        notificationService.notifyAboutTransfer(
                accountFrom, String.format("Your payment (%s) to user %s processed successfully",
                        payment.getAmount(), payment.getTo()));

        log.info("Transaction processed successfully: {}, {}, {}",
                payment.getFrom(), payment.getTo(), payment.getAmount());
    }

}
