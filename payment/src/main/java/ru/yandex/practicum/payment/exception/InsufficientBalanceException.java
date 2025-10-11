package ru.yandex.practicum.payment.exception;

import lombok.Getter;
import ru.yandex.practicum.payment.domain.Balance;

@Getter
public class InsufficientBalanceException extends RuntimeException {
    private final Balance balance;

    public InsufficientBalanceException(Balance balance) {
        this.balance = balance;
    }
}