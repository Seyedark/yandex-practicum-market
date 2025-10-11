package ru.yandex.practicum.market.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponseDto {
    long totalPages;
    List<ItemDto> itemDtoList;
}