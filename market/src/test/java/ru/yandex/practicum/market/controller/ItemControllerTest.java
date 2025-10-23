package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.dto.PageResponseDto;
import ru.yandex.practicum.market.service.CustomUserDetailsService;
import ru.yandex.practicum.market.service.ItemService;
import ru.yandex.practicum.market.service.OrderService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("Класс для проверки взаимодействия с контроллером товаров")
public class ItemControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private ItemService itemService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Проверка метода получения представления главной страницы с авторизацией")
    void getAllItemsByConditionsSuccessTest() {

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


        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(orderService.findCartOrder(1L, false)).thenReturn(orderWithItemsDtoMono);
        when(itemService.getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, pageSize)).thenReturn(pageResponseDtoMono);

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/")
                        .queryParam("search", search)
                        .queryParam("sortBy", sort)
                        .queryParam("page", Integer.toString(page))
                        .queryParam("pageSize", Integer.toString(pageSize))
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(customUserDetailsService, times(1)).getUserIdFromPrincipal(any());
        verify(orderService, times(1)).findCartOrder(1L, false);
        verify(itemService, times(1)).getAllItemsByConditions(orderWithItemsDtoMono, search, sort, page, pageSize);
    }


    @Test
    @DisplayName("Проверка метода получения представления страницы товара с авторизацией")
    void getItemBySuccessIdTest() {

        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        Long id = 1L;

        ItemDto itemDto = new ItemDto();
        itemDto.setImageBase64(Base64.getEncoder().encodeToString("".getBytes(StandardCharsets.UTF_8)));

        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(orderService.findCartOrder(1L, false)).thenReturn(orderWithItemsDtoMono);
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