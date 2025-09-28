package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.ActionEnum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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

    @MockBean
    private OrderItemService orderItemService;

    @Test
    @DisplayName("Проверка когда первый раз добавляем товар")
    void changeItemsInOrderFirstItemTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setId(1L);
        orderWithItemsDto.setTotalAmount(BigDecimal.ZERO);
        orderWithItemsDto.setItemList(new ArrayList<>());
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setTotalAmount(BigDecimal.ZERO);
        Mono<OrderEntity> orderEntityMono = Mono.just(orderEntity);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);
        Mono<ItemEntity> itemEntityMono = Mono.just(itemEntity);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        Mono<OrderItemEntity> orderItemEntityMono = Mono.just(orderItemEntity);

        when(orderService.findCartOrder()).thenReturn(orderWithItemsDtoMono);
        when(itemService.findById(itemEntity.getId())).thenReturn(itemEntityMono);
        when(orderService.save(any(OrderEntity.class))).thenReturn(orderEntityMono);
        when(orderItemService.save(any(OrderItemEntity.class))).thenReturn(orderItemEntityMono);

        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.PLUS.name()).block();

        verify(orderService, times(1)).findCartOrder();
        verify(itemService, times(1)).findById(itemEntity.getId());
        verify(orderService, times(1)).save(any(OrderEntity.class));
        verify(orderItemService, times(1)).save(any(OrderItemEntity.class));

    }

    @Test
    @DisplayName("Проверка когда добавляем товар, который уже был")
    void changeItemsInOrderExistItemIncreaseTest() {
        ItemDto itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setPrice(new BigDecimal(BigInteger.ONE));

        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setId(1L);
        orderWithItemsDto.setTotalAmount(BigDecimal.TWO);
        orderWithItemsDto.setItemList(List.of(itemDto));
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        OrderEntity orderEntity = new OrderEntity();
        Mono<OrderEntity> orderEntityMono = Mono.just(orderEntity);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);
        Mono<ItemEntity> itemEntityMono = Mono.just(itemEntity);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setQuantity(0);
        Mono<OrderItemEntity> orderItemEntityMono = Mono.just(orderItemEntity);


        when(orderService.findCartOrder()).thenReturn(orderWithItemsDtoMono);
        when(itemService.findById(itemEntity.getId())).thenReturn(itemEntityMono);
        when(orderItemService.findByOrderIdAndItemId(orderWithItemsDto.getId(), itemDto.getId())).thenReturn(orderItemEntityMono);
        when(orderService.save(any(OrderEntity.class))).thenReturn(orderEntityMono);
        when(orderItemService.save(any(OrderItemEntity.class))).thenReturn(orderItemEntityMono);


        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.PLUS.name()).block();

        verify(orderService, times(1)).findCartOrder();
        verify(itemService, times(1)).findById(itemEntity.getId());
        verify(orderItemService, times(1)).findByOrderIdAndItemId(orderWithItemsDto.getId(), itemDto.getId());
        verify(orderService, times(1)).save(any(OrderEntity.class));
        verify(orderItemService, times(1)).save(any(OrderItemEntity.class));
    }

    @Test
    @DisplayName("Проверка когда добавляем товар, который уже был")
    void changeItemsInOrderExistItemDecreaseTest() {
        ItemDto itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setPrice(new BigDecimal(BigInteger.ONE));

        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setId(1L);
        orderWithItemsDto.setTotalAmount(BigDecimal.TWO);
        orderWithItemsDto.setItemList(List.of(itemDto));
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        OrderEntity orderEntity = new OrderEntity();
        Mono<OrderEntity> orderEntityMono = Mono.just(orderEntity);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);
        Mono<ItemEntity> itemEntityMono = Mono.just(itemEntity);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setQuantity(1);
        Mono<OrderItemEntity> orderItemEntityMono = Mono.just(orderItemEntity);

        when(orderService.findCartOrder()).thenReturn(orderWithItemsDtoMono);
        when(itemService.findById(itemEntity.getId())).thenReturn(itemEntityMono);
        when(orderItemService.findByOrderIdAndItemId(orderWithItemsDto.getId(), itemDto.getId())).thenReturn(orderItemEntityMono);
        when(orderService.save(any(OrderEntity.class))).thenReturn(orderEntityMono);
        when(orderItemService.save(any(OrderItemEntity.class))).thenReturn(orderItemEntityMono);

        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.MINUS.name()).block();

        verify(orderService, times(1)).findCartOrder();
        verify(itemService, times(1)).findById(itemEntity.getId());
        verify(orderItemService, times(1)).findByOrderIdAndItemId(orderWithItemsDto.getId(), itemDto.getId());
        verify(orderService, times(1)).save(any(OrderEntity.class));
        verify(orderItemService, times(1)).save(any(OrderItemEntity.class));
    }

    @Test
    @DisplayName("Проверка когда добавляем товар, который уже был")
    void changeItemsInOrderExistItemDeleteItemTest() {
        ItemDto itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setPrice(new BigDecimal(BigInteger.ONE));

        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setId(1L);
        orderWithItemsDto.setTotalAmount(BigDecimal.TWO);
        orderWithItemsDto.setItemList(List.of(itemDto));
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        OrderEntity orderEntity = new OrderEntity();
        Mono<OrderEntity> orderEntityMono = Mono.just(orderEntity);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setPrice(BigDecimal.ONE);
        Mono<ItemEntity> itemEntityMono = Mono.just(itemEntity);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setQuantity(1);
        Mono<OrderItemEntity> orderItemEntityMono = Mono.just(orderItemEntity);

        when(orderService.findCartOrder()).thenReturn(orderWithItemsDtoMono);
        when(itemService.findById(itemEntity.getId())).thenReturn(itemEntityMono);
        when(orderItemService.findByOrderIdAndItemId(orderWithItemsDto.getId(), itemDto.getId())).thenReturn(orderItemEntityMono);
        when(orderService.save(any(OrderEntity.class))).thenReturn(orderEntityMono);
        when(orderItemService.delete(any(OrderItemEntity.class))).thenReturn(Mono.empty());

        service.changeItemsInOrder(itemEntity.getId(), ActionEnum.DELETE.name()).block();

        verify(orderService, times(1)).findCartOrder();
        verify(itemService, times(1)).findById(itemEntity.getId());
        verify(orderItemService, times(1)).findByOrderIdAndItemId(orderWithItemsDto.getId(), itemDto.getId());
        verify(orderService, times(1)).save(any(OrderEntity.class));
        verify(orderItemService, times(1)).delete(any(OrderItemEntity.class));
    }
}