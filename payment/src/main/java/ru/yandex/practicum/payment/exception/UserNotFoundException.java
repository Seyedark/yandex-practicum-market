package ru.yandex.practicum.payment.exception;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
    }
}