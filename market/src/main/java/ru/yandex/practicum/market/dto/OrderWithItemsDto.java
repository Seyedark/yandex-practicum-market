package ru.yandex.practicum.market.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderWithItemsDto {
    Long id;
    String status;
    BigDecimal totalAmount;
    List<ItemDto> itemList;
}