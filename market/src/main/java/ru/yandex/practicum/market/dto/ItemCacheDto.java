package ru.yandex.practicum.market.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemCacheDto {
    Long id;
    String name;
    String description;
    BigDecimal price;
    String imageBase64;
}