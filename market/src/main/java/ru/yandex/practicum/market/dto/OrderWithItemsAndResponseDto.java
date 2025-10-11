package ru.yandex.practicum.market.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderWithItemsAndResponseDto {
    OrderWithItemsDto orderWithItemsDto;
    BalanceApiResponseDto balanceApiResponseDto;
}