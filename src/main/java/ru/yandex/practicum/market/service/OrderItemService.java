package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dao.repository.OrderItemRepository;

@Service
@RequiredArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;

    private final R2dbcEntityTemplate entityTemplate;


    public Mono<OrderItemEntity> save(OrderItemEntity orderItemEntity) {
        return orderItemRepository.save(orderItemEntity);
    }

    public Mono<Void> delete(OrderItemEntity orderItemEntity) {
        return orderItemRepository.delete(orderItemEntity);
    }

    public Mono<OrderItemEntity> findByOrderIdAndItemId(Long orderId, Long itemId) {
        return entityTemplate.getDatabaseClient().sql("""
                        SELECT id, orders_id, items_id, quantity
                        FROM orders_items
                        WHERE orders_id= :orderId
                        AND items_id = :itemId
                        """)
                .bind("orderId", orderId)
                .bind("itemId", itemId)
                .fetch()
                .all()
                .collectList()
                .flatMap(rows -> {
                    if (rows.isEmpty()) {
                        return Mono.empty();
                    }
                    var firstRow = rows.get(0);
                    OrderItemEntity orderItemEntity = new OrderItemEntity();
                    orderItemEntity.setId((Long) firstRow.get("id"));
                    orderItemEntity.setOrderId((Long) firstRow.get("orders_id"));
                    orderItemEntity.setItemId((Long) firstRow.get("items_id"));
                    orderItemEntity.setQuantity((Integer) firstRow.get("quantity"));
                    return Mono.just(orderItemEntity);
                });
    }
}