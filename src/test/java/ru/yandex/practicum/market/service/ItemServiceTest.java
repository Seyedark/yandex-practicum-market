package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.market.SpringBootPostgreSQLBase;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dao.repository.OrderItemRepository;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.enums.OrderStatusEnum;
import ru.yandex.practicum.market.enums.SortEnum;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Класс для проверки взаимодействия с сервисом товаров и с базой")
public class ItemServiceTest extends SpringBootPostgreSQLBase {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    ItemService service;

    @BeforeEach
    void cleanup() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Проверка получения данных из БД")
    void findItemByIdTest() {
        ItemEntity actual = service.findById(1L);
        assertNotNull(actual.getId());
        assertNotNull(actual.getName());
        assertNotNull(actual.getDescription());
        assertNotNull(actual.getPrice());
        assertNotNull(actual.getImage());
    }

    @Test
    @DisplayName("Проверка когда для заказа ещё не было связей и вёрнется товар по условия с 0 quantity")
    void getAllItemsByConditionsWithNoOrderTest() {
        OrderEntity orderEntity = new OrderEntity();
        String search = "Корм для кошек";
        String sort = SortEnum.NO.name();
        int page = 1;
        int size = 1;
        Page<ItemEntity> actual = service.getAllItemsByConditions(orderEntity, search, sort, page, size);
        assertEquals(page - 1, actual.getNumber());
        assertEquals(size, actual.getTotalElements());
        assertEquals("Корм для кошек", actual.getContent().get(0).getName());
        assertEquals(0, actual.getContent().get(0).getQuantity());
    }

    @Test
    @DisplayName("Проверка когда для заказа уже были связей и вёрнется товар по условия по условия с 1 quantity")
    void getAllItemsByConditionsWithOrderTest() {

        ItemEntity itemEntity = service.findById(1L);
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(OrderStatusEnum.CART.name());
        orderEntity.setTotalAmount(itemEntity.getPrice());

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrder(orderEntity);
        orderItemEntity.setItem(itemEntity);
        orderItemEntity.setQuantity(1);

        orderRepository.save(orderEntity);

        orderEntity.setOrderItem(List.of(orderItemEntity));

        String search = "Корм для кошек";
        String sort = SortEnum.NO.name();
        int page = 1;
        int size = 1;
        Page<ItemEntity> actual = service.getAllItemsByConditions(orderEntity, search, sort, page, size);
        assertEquals(page - 1, actual.getNumber());
        assertEquals(size, actual.getTotalElements());
        assertEquals("Корм для кошек", actual.getContent().get(0).getName());
        assertEquals(1, actual.getContent().get(0).getQuantity());
    }
}