package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.SpringBootPostgreSQLBase;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@DisplayName("Класс для проверки взаимодействия с сервисом заказов и с базой")
public class OrderServiceTest extends SpringBootPostgreSQLBase {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderService service;

    @BeforeEach
    void cleanup() {
        orderRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Вернёт заказ со статусом корзина")
    void findCartOrderTest() {
        Mono<OrderWithItemsDto> orderWithItemsDtoMonoFirst = service.findCartOrder();
        Mono<OrderWithItemsDto> orderWithItemsDtoMonoSecond = service.findCartOrder();

        assertEquals(OrderStatusEnum.CART.name(), orderWithItemsDtoMonoFirst.block().getStatus());
        assertEquals( orderWithItemsDtoMonoSecond.block().getId(),  orderWithItemsDtoMonoFirst.block().getId());
    }

    @Test
    @DisplayName("Поиск существующего заказа по id")
    void findOrdersWithItemsByStatusTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        Mono<OrderEntity> expectedMono = service.save(orderEntity);
        OrderEntity expected = expectedMono.block();

        Mono<OrderWithItemsDto> orderWithItemsDtoMono = service.findOrdersWithItemsById(expected.getId());

        assertEquals(orderWithItemsDtoMono.block().getId(), expected.getId());
        assertEquals(orderWithItemsDtoMono.block().getStatus(), expected.getStatus());
        assertThat(expected.getTotalAmount()).isEqualByComparingTo(orderWithItemsDtoMono.block().getTotalAmount());
    }

    @Test
    @DisplayName("Сохранение заказа")
    void saveTest() {
        OrderEntity expected = new OrderEntity();
        expected.setStatus(OrderStatusEnum.CART.name());
        expected.setTotalAmount(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        Mono<OrderEntity> actualMono = service.save(expected);
        OrderEntity actual = actualMono.block();

        assertEquals(expected.getStatus(), actual.getStatus());
        assertThat(actual.getTotalAmount()).isEqualByComparingTo(expected.getTotalAmount());
    }

    @Test
    @DisplayName("Поиск списка заказов по статусу")
    void findByStatusTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        Mono<OrderEntity> expectedMono = service.save(orderEntity);
        OrderEntity expected = expectedMono.block();

        Flux<OrderWithItemsDto> orderEntityList = service.findOrdersWithItemsByStatus(OrderStatusEnum.CART.name());

        List<OrderWithItemsDto> orderList = orderEntityList.collectList().block();

        assertEquals(orderList.size(), 1);
        assertEquals(orderList.get(0).getStatus(), OrderStatusEnum.CART.name());
        assertThat(orderList.get(0).getTotalAmount()).isEqualByComparingTo(expected.getTotalAmount());
    }

    @Test
    @DisplayName("Изменение статуса заказа")
    void closeOrderTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        Mono<OrderEntity> expectedMono = service.save(orderEntity);
        OrderEntity expected = expectedMono.block();

        service.closeOrder().block();
        Mono<OrderEntity> changed = orderRepository.findById(expected.getId());
        assertEquals(OrderStatusEnum.ORDER.name(), changed.block().getStatus());
    }
}