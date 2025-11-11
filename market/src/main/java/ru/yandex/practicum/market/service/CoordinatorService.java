package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.ActionEnum;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CoordinatorService {

    private final ItemService itemService;

    private final OrderService orderService;

    private final OrderItemService orderItemService;


    @Transactional
    public Mono<Void> changeItemsInOrder(Long id, String action, Long userId) {
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = orderService.findCartOrder(userId, true);
        return orderWithItemsDtoMono.flatMap(orderWithItemsDto -> {
            ActionEnum actionEnum = ActionEnum.valueOf(action);
            switch (actionEnum) {
                case PLUS:
                    return addItemToOrder(id, Mono.just(orderWithItemsDto));
                case MINUS:
                    return decreaseItemQuantity(id, Mono.just(orderWithItemsDto));
                case DELETE:
                    return removeItemFromOrder(id, Mono.just(orderWithItemsDto));
                default:
                    return Mono.empty();
            }
        });
    }


    private Mono<Void> addItemToOrder(Long id, Mono<OrderWithItemsDto> orderWithItemsDtoMono) {
        return orderWithItemsDtoMono
                .flatMap(orderWithItemsDto -> {
                    if (orderWithItemsDto.getItemList().isEmpty() ||
                            orderWithItemsDto.getItemList().stream().noneMatch(item -> item.getId().equals(id))) {
                        return initializeNewOrderItemEntity(orderWithItemsDto, id);
                    } else {
                        return increaseOrderItemQuantity(orderWithItemsDto, id);
                    }
                });
    }


    private Mono<Void> decreaseItemQuantity(Long id, Mono<OrderWithItemsDto> orderWithItemsDtoMono) {
        return orderWithItemsDtoMono
                .flatMap(orderWithItemsDto -> {
                    Mono<ItemEntity> itemEntityMono = itemService.findById(id);
                    Mono<OrderItemEntity> orderItemEntityMono = orderItemService.findByOrderIdAndItemId(orderWithItemsDto.getId(), id);
                    return Mono.zip(itemEntityMono, orderItemEntityMono)
                            .flatMap(tuple -> {
                                ItemEntity itemEntity = tuple.getT1();
                                OrderItemEntity orderItemEntity = tuple.getT2();
                                if (orderItemEntity.getQuantity() > 1) {
                                    BigDecimal newTotalAmount = orderWithItemsDto.getTotalAmount().subtract(itemEntity.getPrice());

                                    OrderEntity orderEntity = new OrderEntity();
                                    orderEntity.setId(orderWithItemsDto.getId());
                                    orderEntity.setStatus(orderWithItemsDto.getStatus());
                                    orderEntity.setUserId(orderWithItemsDto.getUserId());
                                    orderEntity.setTotalAmount(newTotalAmount);

                                    orderItemEntity.setQuantity(orderItemEntity.getQuantity() - 1);
                                    return orderService.save(orderEntity)
                                            .then(orderItemService.save(orderItemEntity))
                                            .then();
                                } else {
                                    return deleteOrderItemEntity(orderItemEntity, itemEntity, orderWithItemsDto);
                                }
                            });
                });

    }


    private Mono<Void> removeItemFromOrder(Long id, Mono<OrderWithItemsDto> orderWithItemsDtoMono) {
        return orderWithItemsDtoMono
                .flatMap(orderWithItemsDto -> {
                    Mono<ItemEntity> itemEntityMono = itemService.findById(id);
                    Mono<OrderItemEntity> orderItemEntityMono = orderItemService.findByOrderIdAndItemId(orderWithItemsDto.getId(), id);
                    return Mono.zip(itemEntityMono, orderItemEntityMono)
                            .flatMap(tuple -> {
                                ItemEntity itemEntity = tuple.getT1();
                                OrderItemEntity orderItemEntity = tuple.getT2();
                                return deleteOrderItemEntity(orderItemEntity, itemEntity, orderWithItemsDto);
                            });
                });
    }

    private Mono<Void> initializeNewOrderItemEntity(OrderWithItemsDto orderWithItemsDto, Long itemId) {
        Mono<ItemEntity> itemEntityMono = itemService.findById(itemId);
        return itemEntityMono.flatMap(itemEntity -> {
            BigDecimal newTotalAmount = orderWithItemsDto.getTotalAmount().add(itemEntity.getPrice());
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(orderWithItemsDto.getId());
            orderEntity.setUserId(orderWithItemsDto.getUserId());
            orderEntity.setStatus(orderWithItemsDto.getStatus());
            orderEntity.setTotalAmount(newTotalAmount);

            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setItemId(itemId);
            orderItemEntity.setOrderId(orderWithItemsDto.getId());
            orderItemEntity.setQuantity(1);
            return orderService.save(orderEntity)
                    .then(orderItemService.save(orderItemEntity))
                    .then();
        });
    }

    private Mono<Void> increaseOrderItemQuantity(OrderWithItemsDto orderWithItemsDto, Long itemId) {
        Mono<ItemEntity> itemEntityMono = itemService.findById(itemId);
        Mono<OrderItemEntity> orderItemEntityMono = orderItemService.findByOrderIdAndItemId(orderWithItemsDto.getId(), itemId);
        return Mono.zip(itemEntityMono, orderItemEntityMono)
                .flatMap(tuple -> {
                    ItemEntity itemEntity = tuple.getT1();
                    OrderItemEntity orderItemEntity = tuple.getT2();

                    BigDecimal newTotalAmount = orderWithItemsDto.getTotalAmount().add(itemEntity.getPrice());

                    OrderEntity orderEntity = new OrderEntity();
                    orderEntity.setId(orderWithItemsDto.getId());
                    orderEntity.setStatus(orderWithItemsDto.getStatus());
                    orderEntity.setUserId(orderWithItemsDto.getUserId());
                    orderEntity.setTotalAmount(newTotalAmount);

                    orderItemEntity.setQuantity(orderItemEntity.getQuantity() + 1);
                    return orderService.save(orderEntity)
                            .then(orderItemService.save(orderItemEntity))
                            .then();
                });
    }

    private Mono<Void> deleteOrderItemEntity(OrderItemEntity orderItemEntity, ItemEntity itemEntity, OrderWithItemsDto orderWithItemsDto) {
        BigDecimal newTotalAmount = orderWithItemsDto.getTotalAmount()
                .subtract(itemEntity.getPrice()
                        .multiply(BigDecimal.valueOf(orderItemEntity.getQuantity())));

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(orderWithItemsDto.getId());
        orderEntity.setStatus(orderWithItemsDto.getStatus());
        orderEntity.setUserId(orderWithItemsDto.getUserId());
        orderEntity.setTotalAmount(newTotalAmount);

        return orderService.save(orderEntity)
                .then(orderItemService.delete(orderItemEntity))
                .then();
    }
}