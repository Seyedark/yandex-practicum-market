package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.dto.PageResponseDto;
import ru.yandex.practicum.market.service.ItemService;
import ru.yandex.practicum.market.service.OrderService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.*;


@WebFluxTest(ItemController.class)
@ActiveProfiles("test")
@DisplayName("Класс для проверки взаимодействия с контроллером товаров")
public class ItemControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private ItemService itemService;
    @MockBean
    private OrderService orderService;


    @Test
    @DisplayName("Проверка метода получения представления главной страницы")
    void getAllItemsByConditionsTest() {

        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);
        ItemDto itemDto = new ItemDto();

        itemDto.setImageBase64(Base64.getEncoder().encodeToString("".getBytes(StandardCharsets.UTF_8)));

        int page = 1;
        int pageSize = 10;
        String search = "Кот";
        String sort = "NO";

        List<ItemDto> itemDtoList = List.of(itemDto);

        PageResponseDto pageResponseDto = new PageResponseDto();
        pageResponseDto.setItemDtoList(itemDtoList);
        pageResponseDto.setTotalPages(pageSize);
        Mono<PageResponseDto> pageResponseDtoMono = Mono.just(pageResponseDto);

        when(orderService.findCartOrder(false)).thenReturn(orderWithItemsDtoMono);
        when(itemService.getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, pageSize)).thenReturn(pageResponseDtoMono);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/")
                        .queryParam("search", search)
                        .queryParam("sortBy", sort)
                        .queryParam("page", Integer.toString(page))
                        .queryParam("pageSize", Integer.toString(pageSize))
                        .build())
                .exchange()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findCartOrder(false);
        verify(itemService, times(1)).getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, pageSize);
    }

    @Test
    @DisplayName("Проверка метода получения представления страницы товара")
    void getItemByIdTest() {

        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        Long id = 1L;

        ItemDto itemDto = new ItemDto();
        itemDto.setImageBase64(Base64.getEncoder().encodeToString("".getBytes(StandardCharsets.UTF_8)));

        when(orderService.findCartOrder(false)).thenReturn(orderWithItemsDtoMono);
        when(itemService.findByIdWithQuantity(orderWithItemsDtoMono, id)).thenReturn(Mono.just(itemDto));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/item")
                        .queryParam("id", id.toString())
                        .build())
                .exchange()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(itemService, times(1)).findByIdWithQuantity(orderWithItemsDtoMono, id);
    }
}