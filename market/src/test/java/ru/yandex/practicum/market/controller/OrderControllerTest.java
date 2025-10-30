package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.BalanceApiResponseDto;
import ru.yandex.practicum.market.dto.OrderWithItemsAndResponseDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.ActionEnum;
import ru.yandex.practicum.market.enums.BalanceApiEnum;
import ru.yandex.practicum.market.enums.OrderStatusEnum;
import ru.yandex.practicum.market.service.CoordinatorService;
import ru.yandex.practicum.market.service.CustomUserDetailsService;
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
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения представления корзины c переданным пользователем")
    void getCartSuccessTest() {

        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());

        BalanceApiResponseDto balanceApiResponseDto = new BalanceApiResponseDto();
        balanceApiResponseDto.setCode(BalanceApiEnum.INSUFFICIENT_BALANCE_ERROR.getCode());
        balanceApiResponseDto.setErrorMessage(BalanceApiEnum.INSUFFICIENT_BALANCE_ERROR.getErrorMessage());

        OrderWithItemsAndResponseDto orderWithItemsAndResponseDto = new OrderWithItemsAndResponseDto();
        orderWithItemsAndResponseDto.setOrderWithItemsDto(orderWithItemsDto);
        orderWithItemsAndResponseDto.setBalanceApiResponseDto(balanceApiResponseDto);

        Mono<OrderWithItemsAndResponseDto> orderWithItemsAndResponseDtoMono = Mono.just(orderWithItemsAndResponseDto);

        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(orderService.findCartOrderAndCheck(1L)).thenReturn(orderWithItemsAndResponseDtoMono);

        webTestClient
                .mutateWith(
                        SecurityMockServerConfigurers.mockAuthentication(
                                new UsernamePasswordAuthenticationToken("test", null, List.of())
                        ))
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);
        verify(orderService, times(1)).findCartOrderAndCheck(1L);
    }

    @Test
    @DisplayName("Проверка метода получения представления корзины c без пользователя")
    void getCartFailTest() {
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart")
                        .build())
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .valueMatches("Location", ".*/login");
        verify(orderService, times(0)).findCartOrderAndCheck(any());
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода изменения кол-ва товара в заказе c переданным пользователем и редиректа на главную страницу")
    void changeItemsInOrderSuccessTest() {
        Long id = 1L;
        String action = ActionEnum.PLUS.name();
        String form = "main";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", action);
        formData.add("form", form);

        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(coordinatorService.changeItemsInOrder(id, action, 1L)).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(
                        SecurityMockServerConfigurers.mockAuthentication(
                                new UsernamePasswordAuthenticationToken("test", null, List.of())
                        ))
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri("/cart/items/{id}", id)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches("location", "/"); // Проверяем редирект

        verify(coordinatorService, times(1)).changeItemsInOrder(id, action, 1L);
    }

    @Test
    @DisplayName("Проверка метода изменения кол-ва товара в заказе без пользователя")
    void changeItemsInOrderFailTest() {
        Long id = 1L;
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri("/cart/items/{id}", id)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches("Location", ".*/login");

        verify(coordinatorService, times(0)).changeItemsInOrder(any(), any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода изменения кол-ва товара в заказе без csrf токена")
    void changeItemsInOrderFailCsrfTest() {
        Long id = 1L;
        webTestClient
                .post()
                .uri("/cart/items/{id}", id)
                .exchange()
                .expectStatus().is4xxClientError();
        verify(coordinatorService, times(0)).changeItemsInOrder(any(), any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения представления завершенного пустого")
    void buyWithRedirectTest() {
        BalanceApiResponseDto balanceApiResponseDto = new BalanceApiResponseDto();
        balanceApiResponseDto.setCode(BalanceApiEnum.INSUFFICIENT_BALANCE_ERROR.getCode());
        balanceApiResponseDto.setErrorMessage(BalanceApiEnum.INSUFFICIENT_BALANCE_ERROR.getErrorMessage());

        OrderWithItemsAndResponseDto orderWithItemsAndResponseDto = new OrderWithItemsAndResponseDto();
        orderWithItemsAndResponseDto.setBalanceApiResponseDto(balanceApiResponseDto);

        Mono<OrderWithItemsAndResponseDto> orderWithItemsAndResponseDtoMono = Mono.just(orderWithItemsAndResponseDto);

        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(orderService.closeOrder(1L)).thenReturn(orderWithItemsAndResponseDtoMono);

        webTestClient
                .mutateWith(
                        SecurityMockServerConfigurers.mockAuthentication(
                                new UsernamePasswordAuthenticationToken("test", null, List.of())))
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/buy")
                        .build())
                .exchange()
                .expectStatus()
                .is3xxRedirection();


        verify(orderService, times(1)).closeOrder(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения представления завершенного заказа")
    void buyTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());

        OrderWithItemsAndResponseDto orderWithItemsAndResponseDto = new OrderWithItemsAndResponseDto();
        orderWithItemsAndResponseDto.setOrderWithItemsDto(orderWithItemsDto);

        Mono<OrderWithItemsAndResponseDto> orderWithItemsAndResponseDtoMono = Mono.just(orderWithItemsAndResponseDto);

        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(orderService.closeOrder(1L)).thenReturn(orderWithItemsAndResponseDtoMono);

        webTestClient
                .mutateWith(
                        SecurityMockServerConfigurers.mockAuthentication(
                                new UsernamePasswordAuthenticationToken("test", null, List.of())))
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/buy")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);


        verify(orderService, times(1)).closeOrder(1L);
    }

    @Test
    @DisplayName("Проверка метода получения представления завершенного заказа без пользователя")
    void buyFailTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());

        OrderWithItemsAndResponseDto orderWithItemsAndResponseDto = new OrderWithItemsAndResponseDto();
        orderWithItemsAndResponseDto.setOrderWithItemsDto(orderWithItemsDto);

        Mono<OrderWithItemsAndResponseDto> orderWithItemsAndResponseDtoMono = Mono.just(orderWithItemsAndResponseDto);

        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(orderService.closeOrder(1L)).thenReturn(orderWithItemsAndResponseDtoMono);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/buy")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches("Location", ".*/login");
        verify(orderService, times(0)).closeOrder(any());
    }

    @Test
    @DisplayName("Проверка метода получения представления завершенного заказа без csrf токена")
    void buyFailCsrf() {
        webTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/buy")
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError();
        verify(orderService, times(0)).closeOrder(any());
    }


    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения представления всех завершенных заказов")
    void getAllOrdersTest() {
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());

        Flux<OrderWithItemsDto> orderWithItemsDtoFlux = Flux.fromIterable(List.of(orderWithItemsDto));

        when(customUserDetailsService.getUserIdFromPrincipal(any())).thenReturn(Mono.just(1L));
        when(orderService.findOrdersWithItemsByStatus(OrderStatusEnum.ORDER.name(), true, 1L)).thenReturn(orderWithItemsDtoFlux);

        webTestClient
                .mutateWith(
                        SecurityMockServerConfigurers.mockAuthentication(
                                new UsernamePasswordAuthenticationToken("test", null, List.of())))
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findOrdersWithItemsByStatus(OrderStatusEnum.ORDER.name(), true, 1L);
    }

    @Test
    @DisplayName("Проверка метода получения представления всех завершенных заказов без пользователя")
    void getAllOrdersFailTest() {
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches("Location", ".*/login");

        verify(orderService, times(0)).findOrdersWithItemsByStatus(eq(OrderStatusEnum.ORDER.name()), eq(true), any());
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения конкретного завершенного заказа с пользователем")
    void getClosedOrderTest() {
        Long id = 1L;
        OrderWithItemsDto orderWithItemsDto = new OrderWithItemsDto();
        orderWithItemsDto.setTotalAmount(BigDecimal.ONE);
        orderWithItemsDto.setItemList(new ArrayList<>());

        Mono<OrderWithItemsDto> orderWithItemsDtoMono = Mono.just(orderWithItemsDto);


        when(orderService.findOrdersWithItemsById(id)).thenReturn(orderWithItemsDtoMono);

        webTestClient
                .mutateWith(
                        SecurityMockServerConfigurers.mockAuthentication(
                                new UsernamePasswordAuthenticationToken("test", null, List.of())))
                .get()
                .uri("/orders/{id}", id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);
        verify(orderService, times(1)).findOrdersWithItemsById(id);
    }

    @Test
    @DisplayName("Проверка метода получения конкретного завершенного заказа без пользователя")
    void getClosedOrderFailTest() {
        Long id = 1L;
        webTestClient
                .get()
                .uri("/orders/{id}", id)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches("Location", ".*/login");
        verify(orderService, times(0)).findOrdersWithItemsById(id);
    }
}