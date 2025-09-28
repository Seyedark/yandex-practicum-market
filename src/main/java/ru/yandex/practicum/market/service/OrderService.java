package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final R2dbcEntityTemplate entityTemplate;

    @Transactional
    public Mono<OrderWithItemsDto> findCartOrder() {
        return findOrdersWithItemsByStatus(OrderStatusEnum.CART.name()).next()
                .switchIfEmpty(createNewCartOrder());
    }

    @Transactional
    public Mono<OrderWithItemsDto> closeOrder() {
        return findOrdersWithItemsByStatus(OrderStatusEnum.CART.name())
                .next()
                .flatMap(orderWithItemsDto -> {
                    OrderEntity orderEntity = new OrderEntity();

                    orderEntity.setId(orderWithItemsDto.getId());
                    orderEntity.setStatus(OrderStatusEnum.ORDER.name());
                    orderEntity.setTotalAmount(orderWithItemsDto.getTotalAmount());

                    return save(orderEntity)
                            .map(saved -> {
                                orderWithItemsDto.setStatus(OrderStatusEnum.ORDER.name());
                                return orderWithItemsDto;
                            });
                });
    }

    public Flux<OrderWithItemsDto> findOrdersWithItemsByStatus(String status) {
        return entityTemplate.getDatabaseClient().sql("""
                        SELECT o.id, o.status, o.total_amount,
                               oi.quantity, i.id as item_id, i.name, i.description, i.price, i.image
                        FROM orders o
                        LEFT JOIN orders_items oi ON o.id = oi.orders_id
                        LEFT JOIN items i ON oi.items_id = i.id
                        WHERE o.status = :status
                        """)
                .bind("status", status)
                .fetch()
                .all()
                .collectMultimap(row -> row.get("id"))
                .flatMapMany(orderMap -> Flux.fromIterable(orderMap.entrySet()))
                .map(entry -> {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) entry.getValue();
                    Map<String, Object> firstRow = rows.get(0);

                    OrderWithItemsDto order = new OrderWithItemsDto();
                    order.setId((Long) firstRow.get("id"));
                    order.setStatus((String) firstRow.get("status"));
                    order.setTotalAmount((BigDecimal) firstRow.get("total_amount"));

                    List<ItemDto> items = rows.stream()
                            .filter(row -> row.get("name") != null)
                            .map(row -> {
                                ItemDto item = new ItemDto();
                                item.setId((Long) row.get("item_id"));
                                item.setQuantity((Integer) row.get("quantity"));
                                item.setName((String) row.get("name"));
                                item.setDescription((String) row.get("description"));
                                item.setPrice((BigDecimal) row.get("price"));

                                ByteBuffer buffer = (ByteBuffer) row.get("image");
                                if (buffer != null && buffer.hasArray()) {
                                    byte[] imageBytes = buffer.array();
                                    item.setImageBase64(Base64.getEncoder().encodeToString(imageBytes));
                                }
                                return item;
                            })
                            .collect(Collectors.toList());

                    order.setItemList(items);
                    return order;
                });
    }


    public Mono<OrderWithItemsDto> findOrdersWithItemsById(Long id) {
        return entityTemplate.getDatabaseClient().sql("""
                        SELECT o.id, o.status, o.total_amount,
                               oi.quantity, i.id as item_id, i.name, i.description, i.price, i.image
                        FROM orders o
                        LEFT JOIN orders_items oi ON o.id = oi.orders_id
                        LEFT JOIN items i ON oi.items_id = i.id
                        WHERE o.id = :id
                        """)
                .bind("id", id)
                .fetch()
                .all()
                .collectList()
                .map(rows -> {
                    if (rows.isEmpty()) return null;
                    Map<String, Object> firstRow = rows.get(0);
                    OrderWithItemsDto order = new OrderWithItemsDto();
                    order.setId((Long) firstRow.get("id"));
                    order.setStatus((String) firstRow.get("status"));
                    order.setTotalAmount((BigDecimal) firstRow.get("total_amount"));
                    List<ItemDto> items = rows.stream()
                            .filter(row -> row.get("name") != null)
                            .map(row -> {
                                ItemDto item = new ItemDto();
                                item.setId((Long) row.get("item_id"));
                                item.setQuantity((Integer) row.get("quantity"));
                                item.setName((String) row.get("name"));
                                item.setDescription((String) row.get("description"));
                                item.setPrice((BigDecimal) row.get("price"));
                                ByteBuffer buffer = (ByteBuffer) row.get("image");
                                if (buffer.hasArray()) {
                                    byte[] imageBytes = buffer.array();
                                    item.setImageBase64(Base64.getEncoder().encodeToString(imageBytes));
                                }
                                return item;
                            })
                            .collect(Collectors.toList());
                    order.setItemList(items);
                    return order;
                });
    }

    public Mono<OrderEntity> save(OrderEntity orderEntity) {
        return orderRepository.save(orderEntity);
    }


    private Mono<OrderWithItemsDto> createNewCartOrder() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.ZERO);
        return orderRepository.save(orderEntity)
                .flatMap(x -> {
                    OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
                    orderWithItemsDto.setId(x.getId());
                    orderWithItemsDto.setStatus(x.getStatus());
                    orderWithItemsDto.setTotalAmount(x.getTotalAmount());
                    orderWithItemsDto.setItemList(new ArrayList<>());
                    return Mono.just(orderWithItemsDto);
                });
    }
}