package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.enums.ActionEnum;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoordinatorService {

    private final ItemService itemService;

    private final OrderService orderService;


    @Transactional
    public void changeItemsInOrder(Long id, String action) {
        OrderEntity orderEntity = orderService.findCartOrder();
        if (action.equals(ActionEnum.PLUS.name())) {
            OrderItemEntity orderItemEntity;
            if (orderEntity.getOrderItem().isEmpty()) {
                orderItemEntity = initializeNewOrderItemEntity(id, orderEntity);
                orderEntity.getOrderItem().add(orderItemEntity);
            } else {
                Optional<OrderItemEntity> optionalOrderItemEntity = orderEntity
                        .getOrderItem()
                        .stream()
                        .filter(x -> x.getItem().getId().equals(id))
                        .findFirst();
                if (optionalOrderItemEntity.isPresent()) {
                    orderItemEntity = optionalOrderItemEntity.get();
                    orderItemEntity.setQuantity(orderItemEntity.getQuantity() + 1);
                    orderEntity.setTotalAmount(orderEntity.getTotalAmount().add(orderItemEntity.getItem().getPrice()));
                } else {
                    orderItemEntity = initializeNewOrderItemEntity(id, orderEntity);
                    orderEntity.getOrderItem().add(orderItemEntity);
                }
            }
            orderService.save(orderEntity);
        } else if (action.equals(ActionEnum.MINUS.name())) {
            decreaseQuantity(id, orderEntity);
        } else {
            Optional<OrderItemEntity> optionalOrderItemEntity = orderEntity
                    .getOrderItem()
                    .stream()
                    .filter(x -> x.getItem().getId().equals(id) &&
                            x.getOrder().getId().equals(orderEntity.getId()))
                    .findFirst();
            deleteOrderItemEntity(orderEntity, optionalOrderItemEntity.get());
        }
    }

    public ItemEntity getItemById(Long id) {
        ItemEntity itemEntity = itemService.findById(id);
        OrderEntity orderEntity = orderService.findCartOrder();
        Optional<OrderItemEntity> orderItemEntity = itemEntity.getOrderItems().stream()
                .filter(x -> Objects.equals(x.getOrder().getId(), orderEntity.getId()))
                .findFirst();
        if (orderItemEntity.isPresent()) {
            itemEntity.setQuantity(orderItemEntity.get().getQuantity());
        } else {
            itemEntity.setQuantity(0);
        }
        return itemEntity;
    }

    private OrderItemEntity initializeNewOrderItemEntity(Long id, OrderEntity orderEntity) {
        OrderItemEntity orderItemEntity;
        ItemEntity itemEntity = itemService.findById(id);
        orderEntity.setTotalAmount(orderEntity.getTotalAmount().add(itemEntity.getPrice()));

        orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrder(orderEntity);
        orderItemEntity.setItem(itemEntity);
        orderItemEntity.setQuantity(1);
        return orderItemEntity;
    }

    private void deleteOrderItemEntity(OrderEntity orderEntity, OrderItemEntity orderItemEntity) {
        BigDecimal decreaseAmount = orderItemEntity.getItem().getPrice()
                .multiply(BigDecimal.valueOf(orderItemEntity.getQuantity()));
        orderEntity.setTotalAmount(orderEntity.getTotalAmount().subtract(decreaseAmount));
        orderEntity.getOrderItem().remove(orderItemEntity);
        orderService.save(orderEntity);
    }

    private void decreaseQuantity(Long id, OrderEntity orderEntity) {
        Optional<OrderItemEntity> optionalOrderItemEntity = orderEntity
                .getOrderItem()
                .stream()
                .filter(x -> x.getItem().getId().equals(id) &&
                        x.getOrder().getId().equals(orderEntity.getId()))
                .findFirst();
        if (optionalOrderItemEntity.isPresent()) {
            OrderItemEntity orderItemEntity = optionalOrderItemEntity.get();
            int newQuantity = orderItemEntity.getQuantity() - 1;
            if (newQuantity > 0) {
                orderItemEntity.setQuantity(newQuantity);
                orderEntity.setTotalAmount(orderEntity.getTotalAmount().subtract(orderItemEntity.getItem().getPrice()));
                orderService.save(orderEntity);
            } else {
                deleteOrderItemEntity(orderEntity, orderItemEntity);
            }
        }
    }
}