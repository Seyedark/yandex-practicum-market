package ru.yandex.practicum.market.dao.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("orders_items")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemEntity {
    @Id
    @Column("id")
    Long id;
    @Column("orders_id")
    Long orderId;
    @Column("items_id")
    Long itemId;
    @Column("quantity")
    Integer quantity;
}