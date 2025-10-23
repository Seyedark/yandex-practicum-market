package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.dto.ItemCacheDto;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsAndResponseDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.BalanceApiEnum;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final PaymentApiService paymentApiService;
    private final RedisCacheService redisCacheService;

    @Transactional
    public Mono<OrderWithItemsAndResponseDto> findCartOrderAndCheck(Long userId) {
        return findOrdersWithItemsByStatus(OrderStatusEnum.CART.name(), false, userId)
                .next()
                .flatMap(order -> {
                    if (!order.getItemList().isEmpty()) {
                        return Mono.zip(
                                Mono.just(order),
                                redisCacheService.getAllItems().collectList(),
                                paymentApiService.formBalanceResponse(order.getTotalAmount(), userId, true)
                        ).map(tuple -> {
                            OrderWithItemsAndResponseDto dto = new OrderWithItemsAndResponseDto();
                            enrichOrderWithItemsByCashedItems(tuple.getT1(), tuple.getT2());
                            dto.setOrderWithItemsDto(tuple.getT1());
                            dto.setBalanceApiResponseDto(tuple.getT3());
                            return dto;
                        });
                    } else {
                        OrderWithItemsAndResponseDto dto = new OrderWithItemsAndResponseDto();
                        dto.setOrderWithItemsDto(order);
                        return Mono.just(dto);
                    }
                }).switchIfEmpty(createNewCartOrder(userId)
                        .map(newOrder -> {
                            OrderWithItemsAndResponseDto dto = new OrderWithItemsAndResponseDto();
                            dto.setOrderWithItemsDto(newOrder);
                            dto.setBalanceApiResponseDto(null);
                            return dto;
                        })
                );
    }


    private void enrichOrderWithItemsByCashedItems(OrderWithItemsDto orderWithItemsDto, List<ItemCacheDto> itemCacheDtoList) {
        Map<Long, ItemCacheDto> cacheMap = itemCacheDtoList.stream()
                .collect(Collectors.toMap(ItemCacheDto::getId, Function.identity()));

        orderWithItemsDto.getItemList().forEach(itemDto -> {
            ItemCacheDto cacheDto = cacheMap.get(itemDto.getId());
            if (cacheDto != null) {
                itemDto.setName(cacheDto.getName());
                itemDto.setDescription(cacheDto.getDescription());
                itemDto.setPrice(cacheDto.getPrice());
                itemDto.setImageBase64(cacheDto.getImageBase64());
            }
        });
    }

    @Transactional
    public Mono<OrderWithItemsDto> findCartOrder(Long userId, boolean withFullItems) {
        return findOrdersWithItemsByStatus(OrderStatusEnum.CART.name(), withFullItems, userId).next()
                .switchIfEmpty(createNewCartOrder(userId));
    }


    @Transactional
    public Mono<OrderWithItemsAndResponseDto> closeOrder(Long userId) {
        return findOrdersWithItemsByStatus(OrderStatusEnum.CART.name(), true, userId)
                .next()
                .flatMap(orderWithItemsDto -> paymentApiService
                        .formBalanceResponse(orderWithItemsDto.getTotalAmount(), userId, false)
                        .flatMap(balanceApiResponseDto -> {
                            OrderWithItemsAndResponseDto orderWithItemsAndResponseDto = new OrderWithItemsAndResponseDto();
                            if (!balanceApiResponseDto.getCode().equals(BalanceApiEnum.SUCCESS.getCode())) {
                                orderWithItemsAndResponseDto.setBalanceApiResponseDto(balanceApiResponseDto);
                                return Mono.just(orderWithItemsAndResponseDto);
                            } else {
                                OrderEntity orderEntity = new OrderEntity();
                                orderEntity.setId(orderWithItemsDto.getId());
                                orderEntity.setStatus(OrderStatusEnum.ORDER.name());
                                orderEntity.setUserId(userId == -1 ? null : userId);
                                orderEntity.setTotalAmount(orderWithItemsDto.getTotalAmount());
                                return save(orderEntity)
                                        .map(saved -> {
                                            orderWithItemsDto.setStatus(OrderStatusEnum.ORDER.name());
                                            orderWithItemsAndResponseDto.setOrderWithItemsDto(orderWithItemsDto);
                                            return orderWithItemsAndResponseDto;
                                        });
                            }
                        }));
    }


    public Flux<OrderWithItemsDto> findOrdersWithItemsByStatus(String status, boolean withFullItems, Long userId) {
        String sql;
        if (withFullItems) {
            sql = """
                    SELECT o.id, o.user_id, o.status, o.total_amount,
                           oi.quantity, oi.items_id as item_id, i.name, i.description, i.price, i.image
                    FROM orders o
                    LEFT JOIN orders_items oi ON o.id = oi.orders_id
                    LEFT JOIN items i ON oi.items_id = i.id
                    WHERE o.status = :status
                    AND (:userId IS NULL AND o.user_id IS NULL OR o.user_id = :userId)
                    """;
        } else {
            sql = """
                    SELECT o.id, o.user_id, o.status, o.total_amount,
                           oi.quantity, oi.items_id as item_id
                    FROM orders o
                    LEFT JOIN orders_items oi ON o.id = oi.orders_id
                    WHERE o.status = :status
                    AND (:userId IS NULL AND o.user_id IS NULL OR o.user_id = :userId)
                    """;
        }
        DatabaseClient.GenericExecuteSpec spec = entityTemplate.getDatabaseClient().sql(sql)
                .bind("status", status);

        if (userId == -1) {
            spec = spec.bindNull("userId", Long.class);
        } else {
            spec = spec.bind("userId", userId);
        }
        return spec.fetch()
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
                    order.setUserId((Long) firstRow.get("user_id"));
                    List<ItemDto> items = rows.stream()
                            .filter(row -> row.get("item_id") != null)
                            .map(row -> {
                                ItemDto item = new ItemDto();
                                item.setId((Long) row.get("item_id"));
                                item.setQuantity((Integer) row.get("quantity"));
                                if (withFullItems) {
                                    item.setName((String) row.get("name"));
                                    item.setDescription((String) row.get("description"));
                                    item.setPrice((BigDecimal) row.get("price"));

                                    ByteBuffer buffer = (ByteBuffer) row.get("image");
                                    if (buffer != null && buffer.hasArray()) {
                                        byte[] imageBytes = buffer.array();
                                        item.setImageBase64(Base64.getEncoder().encodeToString(imageBytes));
                                    }
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


    private Mono<OrderWithItemsDto> createNewCartOrder(Long userId) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.ZERO);
        orderEntity.setUserId(userId == -1 ? null : userId);
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