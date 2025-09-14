package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.enums.ActionEnum;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = CoordinatorService.class)
@ActiveProfiles("test")
@DisplayName("Класс для проверки взаимодействия с сервиса товаров и сервиса заказов")
public class CoordinatorServiceTest {

    @Autowired
    CoordinatorService service;

    @MockBean
    private ItemService itemService;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("Проверка когда первый раз добавляем товар")
    void changeItemsInOrderFirstItemTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setTotalAmount(BigDecimal.ZERO);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);

        when(orderService.findCartOrder()).thenReturn(orderEntity);
        when(itemService.findById(orderEntity.getId())).thenReturn(itemEntity);

        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.PLUS.name());

        verify(orderService, times(1)).save(orderEntity);
        verify(itemService, times(1)).findById(orderEntity.getId());

        assertEquals(BigDecimal.ONE, orderEntity.getTotalAmount());
        assertEquals(1, orderEntity.getOrderItem().size());
        assertEquals(1, orderEntity.getOrderItem().get(0).getQuantity());
    }

    @Test
    @DisplayName("Проверка когда добавляем товар, который уже был")
    void changeItemsInOrderExistItemIncreaseTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setTotalAmount(BigDecimal.ONE);


        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrder(orderEntity);
        orderItemEntity.setItem(itemEntity);
        orderItemEntity.setQuantity(1);

        orderEntity.getOrderItem().add(orderItemEntity);

        when(orderService.findCartOrder()).thenReturn(orderEntity);
        when(itemService.findById(orderEntity.getId())).thenReturn(itemEntity);

        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.PLUS.name());

        verify(orderService, times(1)).save(orderEntity);

        assertEquals(BigDecimal.TWO, orderEntity.getTotalAmount());
        assertEquals(1, orderEntity.getOrderItem().size());
        assertEquals(2, orderEntity.getOrderItem().get(0).getQuantity());
    }

    @Test
    @DisplayName("Проверка когда добавляем товар, который уже был")
    void changeItemsInOrderExistItemDecreaseTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setTotalAmount(BigDecimal.TWO);


        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrder(orderEntity);
        orderItemEntity.setItem(itemEntity);
        orderItemEntity.setQuantity(2);

        orderEntity.getOrderItem().add(orderItemEntity);

        when(orderService.findCartOrder()).thenReturn(orderEntity);
        when(itemService.findById(orderEntity.getId())).thenReturn(itemEntity);

        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.MINUS.name());

        verify(orderService, times(1)).save(orderEntity);

        assertEquals(BigDecimal.ONE, orderEntity.getTotalAmount());
        assertEquals(1, orderEntity.getOrderItem().size());
        assertEquals(1, orderEntity.getOrderItem().get(0).getQuantity());
    }

    @Test
    @DisplayName("Проверка когда добавляем товар, который уже был")
    void changeItemsInOrderExistItemDeleteItemTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setTotalAmount(BigDecimal.TWO);


        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrder(orderEntity);
        orderItemEntity.setItem(itemEntity);
        orderItemEntity.setQuantity(2);

        orderEntity.getOrderItem().add(orderItemEntity);

        when(orderService.findCartOrder()).thenReturn(orderEntity);
        when(itemService.findById(orderEntity.getId())).thenReturn(itemEntity);

        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.DELETE.name());

        verify(orderService, times(1)).save(orderEntity);

        assertEquals(BigDecimal.ZERO, orderEntity.getTotalAmount());
        assertEquals(0, orderEntity.getOrderItem().size());
    }

    @Test
    @DisplayName("Поиск товара по идентификатору")
    void getItemByIdTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setTotalAmount(BigDecimal.TWO);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrder(orderEntity);
        orderItemEntity.setItem(itemEntity);
        orderItemEntity.setQuantity(2);

        orderEntity.getOrderItem().add(orderItemEntity);
        itemEntity.setOrderItems(List.of(orderItemEntity));

        when(orderService.findCartOrder()).thenReturn(orderEntity);
        when(itemService.findById(orderEntity.getId())).thenReturn(itemEntity);

        service.getItemById(itemEntity.getId());

        verify(orderService, times(1)).findCartOrder();
        verify(itemService, times(1)).findById(orderEntity.getId());


        assertEquals(2, itemEntity.getQuantity());
    }
}