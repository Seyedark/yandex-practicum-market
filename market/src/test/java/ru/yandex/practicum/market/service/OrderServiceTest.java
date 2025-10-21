package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.SpringBootPostgreSQLBase;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dao.repository.OrderItemRepository;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.dto.BalanceApiResponseDto;
import ru.yandex.practicum.market.dto.ItemCacheDto;
import ru.yandex.practicum.market.dto.OrderWithItemsAndResponseDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.BalanceApiEnum;
import ru.yandex.practicum.market.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("Класс для проверки взаимодействия с сервисом заказов и с базой")
public class OrderServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderItemRepository orderItemRepository;
    @Autowired
    OrderService service;
    @MockBean
    private PaymentApiService paymentApiService;
    @MockBean
    private RedisCacheService redisCacheService;

    @BeforeEach
    void cleanup() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Вернёт заказ со статусом корзина и проверит вызовет сервисы платежей и редис")
    void findCartOrderAndCheckTest() {
        Long orderId = 1L;
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());

        OrderEntity savedOrder = orderRepository.save(orderEntity).block();

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderId(savedOrder.getId());
        orderItemEntity.setQuantity(1);
        orderItemEntity.setItemId(1L);

        orderItemRepository.save(orderItemEntity).block();

        BalanceApiResponseDto balanceApiResponseDto = new BalanceApiResponseDto();
        balanceApiResponseDto.setCode(BalanceApiEnum.SUCCESS.getCode());
        Mono<BalanceApiResponseDto> balanceApiResponseDtoMono = Mono.just(balanceApiResponseDto);
        ItemCacheDto itemCacheDto = new ItemCacheDto();
        itemCacheDto.setId(1L);
        itemCacheDto.setPrice(BigDecimal.TEN);
        Flux<ItemCacheDto> itemCacheDtoFlux = Flux.just(itemCacheDto);
        when(paymentApiService.formBalanceResponse(any(), eq(true))).thenReturn(balanceApiResponseDtoMono);
        when(redisCacheService.getAllItems()).thenReturn(itemCacheDtoFlux);

        Mono<OrderWithItemsAndResponseDto> orderWithItemsAndResponseDtoMono = service.findCartOrderAndCheck();
        OrderWithItemsAndResponseDto orderWithItemsAndResponseDto = orderWithItemsAndResponseDtoMono.block();

        assertEquals(OrderStatusEnum.CART.name(), orderWithItemsAndResponseDto.getOrderWithItemsDto().getStatus());
        assertEquals(balanceApiResponseDto.getCode(), orderWithItemsAndResponseDto.getBalanceApiResponseDto().getCode());
    }


    @Test
    @DisplayName("Вернёт заказ со статусом корзина")
    void findCartOrderTest() {
        Mono<OrderWithItemsDto> orderWithItemsDtoMonoFirst = service.findCartOrder(true);
        Mono<OrderWithItemsDto> orderWithItemsDtoMonoSecond = service.findCartOrder(true);
        OrderWithItemsDto first = orderWithItemsDtoMonoFirst.block();
        OrderWithItemsDto second = orderWithItemsDtoMonoSecond.block();
        assertEquals(OrderStatusEnum.CART.name(), first.getStatus());
        assertEquals(second.getId(), first.getId());
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

        OrderWithItemsDto orderWithItemsDto = orderWithItemsDtoMono.block();

        assertEquals(orderWithItemsDto.getId(), expected.getId());
        assertEquals(orderWithItemsDto.getStatus(), expected.getStatus());
        assertThat(expected.getTotalAmount()).isEqualByComparingTo(orderWithItemsDto.getTotalAmount());
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

        Flux<OrderWithItemsDto> orderEntityList = service.findOrdersWithItemsByStatus(OrderStatusEnum.CART.name(), true);

        List<OrderWithItemsDto> orderList = orderEntityList.collectList().block();

        assertEquals(orderList.size(), 1);
        assertEquals(orderList.get(0).getStatus(), OrderStatusEnum.CART.name());
        assertThat(orderList.get(0).getTotalAmount()).isEqualByComparingTo(expected.getTotalAmount());
    }

    @Test
    @DisplayName("Изменение статуса заказа  при положительном ответе от платежного сервиса")
    void closeOrderSuccessTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        Mono<OrderEntity> expectedMono = service.save(orderEntity);
        OrderEntity expected = expectedMono.block();

        BalanceApiResponseDto balanceApiResponseDto = new BalanceApiResponseDto();
        balanceApiResponseDto.setCode(BalanceApiEnum.SUCCESS.getCode());
        Mono<BalanceApiResponseDto> balanceApiResponseDtoMono = Mono.just(balanceApiResponseDto);

        when(paymentApiService.formBalanceResponse(any(), eq(false))).thenReturn(balanceApiResponseDtoMono);

        service.closeOrder().block();

        service.closeOrder().block();
        Mono<OrderEntity> changed = orderRepository.findById(expected.getId());
        assertEquals(OrderStatusEnum.ORDER.name(), changed.block().getStatus());
    }

    @Test
    @DisplayName("Изменение статуса заказа при отрицательном ответе от платежного сервиса")
    void closeOrderErrorTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        Mono<OrderEntity> expectedMono = service.save(orderEntity);
        OrderEntity expected = expectedMono.block();

        BalanceApiResponseDto balanceApiResponseDto = new BalanceApiResponseDto();
        balanceApiResponseDto.setCode(BalanceApiEnum.UNEXPECTED_ERROR.getCode());
        Mono<BalanceApiResponseDto> balanceApiResponseDtoMono = Mono.just(balanceApiResponseDto);

        when(paymentApiService.formBalanceResponse(any(), eq(false))).thenReturn(balanceApiResponseDtoMono);

        service.closeOrder().block();

        Mono<OrderEntity> changed = orderRepository.findById(expected.getId());
        assertEquals(OrderStatusEnum.CART.name(), changed.block().getStatus());
    }
}