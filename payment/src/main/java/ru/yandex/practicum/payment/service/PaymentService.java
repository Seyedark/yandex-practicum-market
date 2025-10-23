package ru.yandex.practicum.payment.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.domain.Balance;
import ru.yandex.practicum.payment.domain.WithdrawBalanceRequest;
import ru.yandex.practicum.payment.exception.InsufficientBalanceException;
import ru.yandex.practicum.payment.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    @Value("${app.amount}")
    private BigDecimal amount;

    private final ConcurrentHashMap<Long, BigDecimal> userBalances = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        userBalances.put(1L, amount);
        userBalances.put(2L, amount);
    }

    public Mono<Balance> getBalance(BigDecimal purchaseAmount, Long userId) {
        return Mono.justOrEmpty(userBalances.get(userId))
                .switchIfEmpty(Mono.error(new UserNotFoundException()))
                .flatMap(amount -> {
                    Balance balance = new Balance();
                    balance.setAmount(amount);
                    if (amount.compareTo(purchaseAmount) < 0) {
                        log.error("Баланс пользователя {} меньше суммы списания {}", userId, purchaseAmount);
                        return Mono.error(new InsufficientBalanceException(balance));
                    }
                    return Mono.just(balance);
                });
    }

    public Mono<Balance> withdrawBalance(Mono<WithdrawBalanceRequest> withdrawBalanceRequest) {
        return withdrawBalanceRequest
                .flatMap(request -> Mono.justOrEmpty(userBalances.get(request.getUserId()))
                        .switchIfEmpty(Mono.error(new UserNotFoundException()))
                        .flatMap(amount -> {
                            if (amount.compareTo(request.getAmount()) < 0) {
                                Balance balance = new Balance();
                                balance.setAmount(amount);
                                log.error("Баланс меньше суммы списания");
                                return Mono.error(new InsufficientBalanceException(balance));
                            } else {
                                BigDecimal newAmount = amount.subtract(request.getAmount());
                                userBalances.put(request.getUserId(), newAmount);
                                Balance balance = new Balance();
                                balance.setAmount(newAmount);
                                return Mono.just(balance);
                            }
                        }));
    }
}