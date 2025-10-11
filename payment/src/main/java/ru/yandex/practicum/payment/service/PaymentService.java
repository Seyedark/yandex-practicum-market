package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.domain.Balance;
import ru.yandex.practicum.payment.domain.WithdrawBalanceRequest;
import ru.yandex.practicum.payment.exception.InsufficientBalanceException;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {


    @Value("${app.amount}")
    private BigDecimal amount;

    public Mono<Balance> getBalance(BigDecimal purchaseAmount) {
        Balance balance = new Balance();
        balance.setAmount(amount);
        if (amount.compareTo(purchaseAmount) < 0) {
            log.error("Баланс меньше суммы списания");
            return Mono.error(new InsufficientBalanceException(balance));
        }
        return Mono.just(balance);
    }

    public Mono<Balance> withdrawBalance(Mono<WithdrawBalanceRequest> withdrawBalanceRequest) {
        return withdrawBalanceRequest
                .flatMap(x -> {
                    if (amount.compareTo(x.getAmount()) < 0) {
                        Balance balance = new Balance();
                        balance.setAmount(amount);
                        log.error("Баланс меньше суммы списания");
                        return Mono.error(new InsufficientBalanceException(balance));
                    } else {
                        Balance balance = new Balance();
                        amount = amount.subtract(x.getAmount());
                        balance.setAmount(amount);
                        return Mono.just(balance);
                    }
                });
    }
}