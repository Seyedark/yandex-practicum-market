package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.market.SpringBootPostgreSQLBase;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.SortEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@DisplayName("Класс для проверки взаимодействия с сервисом товаров и с базой")
public class ItemServiceTest extends SpringBootPostgreSQLBase {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ItemService service;


    @Test
    @DisplayName("Проверка получения данных из БД")
    void findItemByIdTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setItemList(new ArrayList<>());
        orderWithItemsDto.setId(1L);
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        ItemDto expectedItemDto = new ItemDto();
        expectedItemDto.setId(1L);
        expectedItemDto.setName("Корм для кошек");
        expectedItemDto.setDescription("Корм для кошек");
        expectedItemDto.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        expectedItemDto.setQuantity(0);


        Mono<ItemDto> actual = service.findByIdWithQuantity(orderWithItemsDtoMono, 1L);
        StepVerifier.create(actual)
                .expectNextMatches(item ->
                        item.getId().equals(expectedItemDto.getId()) &&
                                item.getName().equals(expectedItemDto.getName()) &&
                                item.getPrice().equals(expectedItemDto.getPrice()) &&
                                item.getDescription().equals(expectedItemDto.getDescription()) &&
                                item.getQuantity().equals(expectedItemDto.getQuantity())
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка когда для заказа ещё не было связей и вёрнется товар по условия с 0 quantity")
    void getAllItemsByConditionsWithNoOrderTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setItemList(new ArrayList<>());
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        ItemDto expectedItemDto = new ItemDto();
        expectedItemDto.setId(1L);
        expectedItemDto.setName("Корм для кошек");
        expectedItemDto.setDescription("Корм для кошек");
        expectedItemDto.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        expectedItemDto.setQuantity(0);

        String search = "Корм для кошек";
        String sort = SortEnum.NO.name();
        int page = 1;
        int size = 1;

        Mono<Page<ItemDto>> actual = service.getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, size);


        StepVerifier.create(actual)
                .expectNextMatches(pageItemDto ->
                        pageItemDto.getContent().getFirst().getId().equals(expectedItemDto.getId()) &&
                                pageItemDto.getContent().getFirst().getName().equals(expectedItemDto.getName()) &&
                                pageItemDto.getContent().getFirst().getPrice().equals(expectedItemDto.getPrice()) &&
                                pageItemDto.getContent().getFirst().getDescription().equals(expectedItemDto.getDescription()) &&
                                pageItemDto.getContent().getFirst().getQuantity().equals(expectedItemDto.getQuantity()) &&
                                pageItemDto.getTotalElements() == size
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка когда для заказа уже были связей и вёрнется товар по условия по условия с 1 quantity")
    void getAllItemsByConditionsWithOrderTest() {

        ItemDto expectedItemDto = new ItemDto();
        expectedItemDto.setId(1L);
        expectedItemDto.setName("Корм для кошек");
        expectedItemDto.setDescription("Корм для кошек");
        expectedItemDto.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        expectedItemDto.setQuantity(1);

        String search = "Корм для кошек";
        String sort = SortEnum.NO.name();
        int page = 1;
        int size = 1;


        OrderWithItemsDto orderEntity = new OrderWithItemsDto();
        orderEntity.setItemList(List.of(expectedItemDto));

        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderEntity);
        Mono<Page<ItemDto>> actual = service.getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, size);


        StepVerifier.create(actual)
                .expectNextMatches(pageItemDto ->
                        pageItemDto.getContent().getFirst().getId().equals(expectedItemDto.getId()) &&
                                pageItemDto.getContent().getFirst().getName().equals(expectedItemDto.getName()) &&
                                pageItemDto.getContent().getFirst().getPrice().equals(expectedItemDto.getPrice()) &&
                                pageItemDto.getContent().getFirst().getDescription().equals(expectedItemDto.getDescription()) &&
                                pageItemDto.getContent().getFirst().getQuantity().equals(expectedItemDto.getQuantity()) &&
                                pageItemDto.getTotalElements() == size
                )
                .verifyComplete();
    }
}