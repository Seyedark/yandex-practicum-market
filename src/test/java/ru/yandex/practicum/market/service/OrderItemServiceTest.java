package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.SpringBootPostgreSQLBase;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dao.repository.OrderItemRepository;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Класс для проверки взаимодействия с сервисом связующей таблицы и с базой")
public class OrderItemServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemService service;

    @BeforeEach
    void cleanup() {
        orderItemRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Сохранение связанной сущности")
    void saveTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        Mono<OrderEntity> orderEntityMono = orderRepository.save(orderEntity);
        OrderEntity savedOrderEntity = orderEntityMono.block();

        OrderItemEntity expected = new OrderItemEntity();
        expected.setItemId(1L);
        expected.setOrderId(savedOrderEntity.getId());
        expected.setQuantity(1);
        Mono<OrderItemEntity> actualMono = service.save(expected);
        OrderItemEntity actual = actualMono.block();

        assertEquals(expected.getItemId(), actual.getItemId());
        assertEquals(expected.getOrderId(), actual.getOrderId());
        assertEquals(expected.getQuantity(), actual.getQuantity());
    }


    @Test
    @DisplayName("Удаление связанной сущности и проверка поиска по id")
    void deleteTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        Mono<OrderEntity> orderEntityMono = orderRepository.save(orderEntity);
        OrderEntity savedOrderEntity = orderEntityMono.block();

        OrderItemEntity expected = new OrderItemEntity();
        expected.setItemId(1L);
        expected.setOrderId(savedOrderEntity.getId());
        expected.setQuantity(1);
        Mono<OrderItemEntity> savedMono = service.save(expected);
        savedMono.block();
        Mono<Void> deleteMono = service.delete(expected);
        deleteMono.block();
        OrderItemEntity deletedEntity = service.findByOrderIdAndItemId(1L, 1L)
                .block();

        assertNull(deletedEntity);
    }
}
