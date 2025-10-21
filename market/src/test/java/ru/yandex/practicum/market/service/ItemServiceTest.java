package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.market.SpringBootPostgreSQLBase;
import ru.yandex.practicum.market.dao.repository.OrderRepository;
import ru.yandex.practicum.market.dto.ItemCacheDto;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.dto.PageResponseDto;
import ru.yandex.practicum.market.enums.SortEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@DisplayName("Класс для проверки взаимодействия с сервисом товаров и с базой")
public class ItemServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    ItemService service;
    @MockBean
    private RedisCacheService redisCacheService;


    @Test
    @DisplayName("Проверка получения данных из кеша")
    void findItemByIdTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setItemList(new ArrayList<>());
        orderWithItemsDto.setId(1L);

        ItemDto itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setQuantity(1);
        orderWithItemsDto.setItemList(List.of(itemDto));

        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        ItemCacheDto itemCacheDto = new ItemCacheDto();
        itemCacheDto.setId(1L);
        itemCacheDto.setName("Корм для кошек");
        itemCacheDto.setDescription("Корм для кошек");
        itemCacheDto.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));

        Mono<ItemCacheDto> itemCacheDtoMono = Mono.just(itemCacheDto);

        when(redisCacheService.getItemById(1L)).thenReturn(itemCacheDtoMono);

        Mono<ItemDto> actual = service.findByIdWithQuantity(orderWithItemsDtoMono, 1L);


        StepVerifier.create(actual)
                .expectNextMatches(item ->
                        item.getId().equals(itemCacheDto.getId()) &&
                                item.getName().equals(itemCacheDto.getName()) &&
                                item.getPrice().equals(itemCacheDto.getPrice()) &&
                                item.getDescription().equals(itemCacheDto.getDescription()) &&
                                item.getQuantity().equals(itemDto.getQuantity())
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка когда для заказа ещё не было связей и вёрнется товар по условия с 0 quantity")
    void getAllItemsByConditionsWithNoOrderTest() {
        Long itemId = 1L;
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setItemList(new ArrayList<>());
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        ItemCacheDto itemCacheDto = new ItemCacheDto();
        itemCacheDto.setId(itemId);
        itemCacheDto.setName("Корм для кошек");
        itemCacheDto.setDescription("Корм для кошек");
        itemCacheDto.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));

        Flux<ItemCacheDto> itemCacheDtoMono = Flux.just(itemCacheDto);

        when(redisCacheService.getAllItems()).thenReturn(itemCacheDtoMono);

        String search = "Корм для кошек";
        String sort = SortEnum.NO.name();
        int page = 1;
        int size = 1;

        Mono<PageResponseDto> actual = service.getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, size);


        StepVerifier.create(actual)
                .expectNextMatches(pageResponseDto ->
                        pageResponseDto.getTotalPages() == 1 &&
                                pageResponseDto.getItemDtoList().getFirst().getName().equals(itemCacheDto.getName()) &&
                                pageResponseDto.getItemDtoList().getFirst().getPrice().equals(itemCacheDto.getPrice()) &&
                                pageResponseDto.getItemDtoList().getFirst().getDescription().equals(itemCacheDto.getDescription()) &&
                                pageResponseDto.getItemDtoList().getFirst().getQuantity().equals(0) &&
                                pageResponseDto.getItemDtoList().size() == size
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка когда для заказа уже были связей и вёрнется товар по условия по условия с 1 quantity")
    void getAllItemsByConditionsWithOrderTest() {
        Long itemId = 1L;

        ItemDto expectedItemDto = new ItemDto();
        expectedItemDto.setId(1L);
        expectedItemDto.setName("Корм для кошек");
        expectedItemDto.setDescription("Корм для кошек");
        expectedItemDto.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        expectedItemDto.setQuantity(1);

        ItemCacheDto itemCacheDto = new ItemCacheDto();
        itemCacheDto.setId(itemId);
        itemCacheDto.setName("Корм для кошек");
        itemCacheDto.setDescription("Корм для кошек");
        itemCacheDto.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));

        OrderWithItemsDto orderEntity = new OrderWithItemsDto();
        orderEntity.setItemList(List.of(expectedItemDto));

        String search = "Корм для кошек";
        String sort = SortEnum.NO.name();
        int page = 1;
        int size = 1;
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderEntity);
        Flux<ItemCacheDto> itemCacheDtoMono = Flux.just(itemCacheDto);

        when(redisCacheService.getAllItems()).thenReturn(itemCacheDtoMono);


        Mono<PageResponseDto> actual = service.getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, size);


        StepVerifier.create(actual)
                .expectNextMatches(pageResponseDto ->
                        pageResponseDto.getTotalPages() == 1 &&
                                pageResponseDto.getItemDtoList().getFirst().getId().equals(itemCacheDto.getId()) &&
                                pageResponseDto.getItemDtoList().getFirst().getName().equals(itemCacheDto.getName()) &&
                                pageResponseDto.getItemDtoList().getFirst().getPrice().equals(itemCacheDto.getPrice()) &&
                                pageResponseDto.getItemDtoList().getFirst().getDescription().equals(itemCacheDto.getDescription()) &&
                                pageResponseDto.getItemDtoList().getFirst().getQuantity().equals(expectedItemDto.getQuantity()) &&
                                pageResponseDto.getItemDtoList().size() == size
                )
                .verifyComplete();
    }
}