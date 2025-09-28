package ru.yandex.practicum.market.dao.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("items")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemEntity {
    @Id
    @Column("id")
    Long id;
    @Column("name")
    String name;
    @Column("description")
    String description;
    @Column("price")
    BigDecimal price;
    @Column("image")
    byte[] image;
}