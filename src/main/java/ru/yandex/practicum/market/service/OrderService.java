package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public OrderEntity findCartOrder() {
        List<OrderEntity> orderEntityList = findOrderByStatus(OrderStatusEnum.CART.name());
        if (orderEntityList.isEmpty()) {
            return createNewCartOrder();
        } else {
            if (orderEntityList.size() > 1) {
                throw new RuntimeException();
            }
            return orderEntityList.getFirst();
        }
    }

    public OrderEntity save(OrderEntity orderEntity) {
      return orderRepository.save(orderEntity);
    }


    public List<OrderEntity> findOrderByStatus(String status) {
        return orderRepository.findByStatus(status);
    }


    @Transactional
    public OrderEntity closeOrder() {
        List<OrderEntity> orderEntityList = findOrderByStatus(OrderStatusEnum.CART.name());
        OrderEntity orderEntity = orderEntityList.getFirst();
        orderEntity.setStatus(OrderStatusEnum.ORDER.name());
        return orderRepository.save(orderEntity);
    }


    private OrderEntity createNewCartOrder() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.ZERO);
        return orderRepository.save(orderEntity);
    }

    public OrderEntity findById(Long id) {
        return orderRepository.findById(id).get();
    }
}