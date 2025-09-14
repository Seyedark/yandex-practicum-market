package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.market.SpringBootPostgreSQLBase;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("Класс для проверки взаимодействия с сервисом заказов и с базой")
public class OrderServiceTest extends SpringBootPostgreSQLBase {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderService service;

    @BeforeEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Создаст заказ со статусом корзина если такого не существовало иначе вернёт существующий")
    void findCartOrderTest() {
        OrderEntity orderEntityFirst = service.findCartOrder();
        OrderEntity orderEntitySecond = service.findCartOrder();

        assertEquals(OrderStatusEnum.CART.name(), orderEntityFirst.getStatus());
        assertEquals(orderEntitySecond.getId(), orderEntityFirst.getId());
    }

    @Test
    @DisplayName("Поиск существующего заказа по id")
    void findByIdTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.ONE);
        OrderEntity expected = orderRepository.save(orderEntity);

        OrderEntity actual = service.findById(expected.getId());
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertThat(actual.getTotalAmount()).isEqualByComparingTo(expected.getTotalAmount());
    }

    @Test
    @DisplayName("Сохранение заказа")
    void saveTest() {
        OrderEntity expected = new OrderEntity();
        expected.setStatus(OrderStatusEnum.CART.name());
        expected.setTotalAmount(BigDecimal.ONE);
        OrderEntity actual = service.save(expected);

        assertEquals(expected.getStatus(), actual.getStatus());
        assertThat(actual.getTotalAmount()).isEqualByComparingTo(expected.getTotalAmount());
    }

    @Test
    @DisplayName("Поиск списка заказов по статусу")
    void findByStatusTest() {
        OrderEntity expected = new OrderEntity();
        expected.setStatus(OrderStatusEnum.CART.name());
        expected.setTotalAmount(BigDecimal.ONE);
        orderRepository.save(expected);

        List<OrderEntity> orderEntityList = service.findOrderByStatus(OrderStatusEnum.CART.name());
        assertEquals(orderEntityList.size(), 1);
        assertEquals(orderEntityList.get(0).getStatus(), OrderStatusEnum.CART.name());
        assertThat(orderEntityList.get(0).getTotalAmount()).isEqualByComparingTo(expected.getTotalAmount());
    }

    @Test
    @DisplayName("Изменение статуса заказа")
    void closeOrderTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.ONE);
        OrderEntity saved = orderRepository.save(orderEntity);

        service.closeOrder();
        Optional<OrderEntity> changed = orderRepository.findById(saved.getId());
        assertTrue(changed.isPresent());
        assertEquals(OrderStatusEnum.ORDER.name(), changed.get().getStatus());
    }
}