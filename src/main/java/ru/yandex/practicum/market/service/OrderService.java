package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dao.repository.OrderItemRepository;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.enums.ActionEnum;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ItemService itemService;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderEntity findCartOrder() {
        List<OrderEntity> orderEntityList = findOrderByStatus(OrderStatusEnum.CART.name());
        if (orderEntityList.isEmpty()) {
            return createNewCartOrder();
        } else {
            return orderEntityList.getFirst();
        }
    }

    public List<ItemEntity> findItemsFromCart(OrderEntity orderEntity) {

        return orderEntity.getOrderItems()
                .stream()
                .map(orderItem -> {
                    ItemEntity item = orderItem.getItem();
                    item.setQuantity(orderItem.getQuantity());
                    return item;
                })
                .toList();
    }

    @Transactional
    public void changeItemsInOrder(Long id, String action) {
        OrderEntity orderEntity = findCartOrder();
        if (action.equals(ActionEnum.PLUS.name())) {
            OrderItemEntity orderItemEntity;
            if (orderEntity.getOrderItems().isEmpty()) {
                orderItemEntity = initializeNewOrderItemEntity(id, orderEntity);
            } else {
                Optional<OrderItemEntity> optionalOrderItemEntity = orderEntity
                        .getOrderItems()
                        .stream()
                        .filter(x -> x.getItem().getId().equals(id))
                        .findFirst();
                if (optionalOrderItemEntity.isPresent()) {
                    orderItemEntity = optionalOrderItemEntity.get();
                    orderItemEntity.setQuantity(orderItemEntity.getQuantity() + 1);
                    orderEntity.setTotalAmount(orderEntity.getTotalAmount().add(orderItemEntity.getItem().getPrice()));
                } else {
                    orderItemEntity = initializeNewOrderItemEntity(id, orderEntity);
                }
            }
            orderRepository.save(orderEntity);
            orderItemRepository.save(orderItemEntity);
        } else if (action.equals(ActionEnum.MINUS.name())) {
            decreaseQuantity(id, orderEntity);
        } else {
            Optional<OrderItemEntity> optionalOrderItemEntity = orderEntity
                    .getOrderItems()
                    .stream()
                    .filter(x -> x.getItem().getId().equals(id) &&
                            x.getOrder().getId().equals(orderEntity.getId()))
                    .findFirst();
            deleteOrderItemEntity(orderEntity, optionalOrderItemEntity.get());
        }
    }

    private OrderEntity createNewCartOrder() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.ZERO);
        return orderRepository.save(orderEntity);
    }

    private List<OrderEntity> findOrderByStatus(String status) {
        return orderRepository.findByStatus(status);
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

    private void decreaseQuantity(Long id, OrderEntity orderEntity) {
        Optional<OrderItemEntity> optionalOrderItemEntity = orderEntity
                .getOrderItems()
                .stream()
                .filter(x -> x.getItem().getId().equals(id) &&
                        x.getOrder().getId().equals(orderEntity.getId()))
                .findFirst();
        OrderItemEntity orderItemEntity = optionalOrderItemEntity.get();
        int quantity = orderItemEntity.getQuantity() - 1;
        if (quantity > 0) {
            orderItemEntity.setQuantity(orderItemEntity.getQuantity() - 1);
            orderEntity.setTotalAmount(orderEntity.getTotalAmount().subtract(orderItemEntity.getItem().getPrice()));
            orderRepository.save(orderEntity);
            orderItemRepository.save(orderItemEntity);
        } else {
            deleteOrderItemEntity(orderEntity, orderItemEntity);
        }
    }

    private void deleteOrderItemEntity(OrderEntity orderEntity, OrderItemEntity orderItemEntity) {
        BigDecimal decreaseAmount = orderItemEntity.getItem().getPrice()
                .multiply(BigDecimal.valueOf(orderItemEntity.getQuantity()));
        orderEntity.getOrderItems().remove(orderItemEntity);
        orderEntity.setTotalAmount(orderEntity.getTotalAmount().subtract(decreaseAmount));
        orderItemRepository.delete(orderItemEntity);
    }
}