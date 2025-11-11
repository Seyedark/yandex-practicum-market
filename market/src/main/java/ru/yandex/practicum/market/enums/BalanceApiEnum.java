package ru.yandex.practicum.market.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BalanceApiEnum {
    SUCCESS(200, null),
    USER_NOT_FOUND_ERROR(422, "У текущего пользователя нет счёта"),
    INSUFFICIENT_BALANCE_ERROR(400, "Недостаточно средств на счёте. Текущий баланс = "),
    UNEXPECTED_ERROR(500, "Ошибка в работе сервиса. Повторите попытку позже");
    private final Integer code;
    private final String errorMessage;
}