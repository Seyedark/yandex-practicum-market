package ru.yandex.practicum.market.dao.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;


@Table("orders")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderEntity {
    @Id
    @Column("id")
    Long id;
    @Column("status")
    String status;
    @Column("total_amount")
    BigDecimal totalAmount;
}