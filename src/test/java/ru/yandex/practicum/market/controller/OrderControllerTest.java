package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.ActionEnum;
import ru.yandex.practicum.market.enums.OrderStatusEnum;
import ru.yandex.practicum.market.service.CoordinatorService;
import ru.yandex.practicum.market.service.OrderService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@WebFluxTest(OrderController.class)
@ActiveProfiles("test")
@DisplayName("Класс для проверки взаимодействия с контроллером заказов")
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CoordinatorService coordinatorService;

    @Test
    @DisplayName("Проверка метода получения представления корзины")
    void getCartTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        when(orderService.findCartOrder()).thenReturn(orderWithItemsDtoMono);


        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findCartOrder();
    }

    @Test
    @DisplayName("Проверка метода изменения кол-ва товара в заказе и редиректа на главную страницу")
    void changeItemsInOrderTest() {
        Long id = 1L;
        String action = ActionEnum.PLUS.name();
        String form = "main";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", action);
        formData.add("form", form);

        when(coordinatorService.changeItemsInOrder(id, action)).thenReturn(Mono.empty());


        webTestClient.post()
                .uri("/cart/items/{id}", id)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches("location", "/"); // Проверяем редирект

        verify(coordinatorService, times(1)).changeItemsInOrder(id, action);
    }

    @Test
    @DisplayName("Проверка метода получения представления завершенного заказа")
    void buyTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());
        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);

        when(orderService.closeOrder()).thenReturn(orderWithItemsDtoMono);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/buy")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);


        verify(orderService, times(1)).closeOrder();
    }

    @Test
    @DisplayName("Проверка метода получения представления всех завершенных заказов")
    void getAllOrdersTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());

        Flux<OrderWithItemsDto> orderWithItemsDtoFlux = Flux.fromIterable(List.of(orderWithItemsDto));

        when(orderService.findOrdersWithItemsByStatus(OrderStatusEnum.ORDER.name())).thenReturn(orderWithItemsDtoFlux);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findOrdersWithItemsByStatus(OrderStatusEnum.ORDER.name());
    }

    @Test
    @DisplayName("Проверка метода получения конкретного завершенного заказа")
    void getClosedOrderTest() {
        Long id = 1L;
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());

        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);


        when(orderService.findOrdersWithItemsById(id)).thenReturn(orderWithItemsDtoMono);

        webTestClient.get()
                .uri("/orders/{id}", id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findOrdersWithItemsById(id);
    }
}