package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.domain.Balance;
import ru.yandex.practicum.payment.domain.WithdrawBalanceRequest;

import java.math.BigDecimal;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты для проверки всей логики")
public class BalanceControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @Value("${app.amount}")
    private BigDecimal amount;


    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения баланса с успешным статусом")
    void getBalanceWithSuccessResponseTest() {
        Long userId = 1L;
        BigDecimal purchaseAmount = BigDecimal.valueOf(100);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/balance")
                        .queryParam("purchaseAmount", purchaseAmount)
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Balance.class);
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения баланса с ошибочным статусом когда не хватает денег")
    void getBalanceWithErrorResponseTest() {
        Long userId = 1L;
        BigDecimal purchaseAmount = BigDecimal.valueOf(10000);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/balance")
                        .queryParam("purchaseAmount", purchaseAmount)
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Balance.class);
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода получения баланса с ошибочным статусом когда нет такого пользователя")
    void getBalanceWithUserErrorResponseTest() {
        Long userId = 3L;
        BigDecimal purchaseAmount = BigDecimal.valueOf(100);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/balance")
                        .queryParam("purchaseAmount", purchaseAmount)
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody(Balance.class);
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода списания с успешным статусом")
    void withdrawBalanceWithSuccessResponseTest() {
        Long userId = 1L;
        WithdrawBalanceRequest request = new WithdrawBalanceRequest();
        BigDecimal purchaseAmount = BigDecimal.valueOf(100);
        request.setUserId(userId);
        request.setAmount(purchaseAmount);

        webTestClient
                .mutateWith(csrf())
                .put()
                .uri(uriBuilder -> uriBuilder.path("/balance/withdraw")
                        .build())
                .body(Mono.just(request), WithdrawBalanceRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Balance.class)
                .value(balance -> Assertions.assertEquals(balance.getAmount(), amount.subtract(purchaseAmount)));
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода списания с ошибочным статусом когда не хватает денег")
    void withdrawBalanceWithErrorResponseTest() {
        Long userId = 1L;
        WithdrawBalanceRequest request = new WithdrawBalanceRequest();
        BigDecimal purchaseAmount = BigDecimal.valueOf(10000);
        request.setUserId(userId);
        request.setAmount(purchaseAmount);

        webTestClient
                .mutateWith(csrf())
                .put()
                .uri(uriBuilder -> uriBuilder.path("/balance/withdraw")
                        .build())
                .body(Mono.just(request), WithdrawBalanceRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Balance.class)
                .value(balance -> Assertions.assertEquals(balance.getAmount(), amount));
    }

    @Test
    @WithMockUser
    @DisplayName("Проверка метода списания с ошибочным статусом когда нет пользователя")
    void withdrawBalanceWithUserErrorResponseTest() {
        Long userId = 3L;
        WithdrawBalanceRequest request = new WithdrawBalanceRequest();
        BigDecimal purchaseAmount = BigDecimal.valueOf(100);
        request.setUserId(userId);
        request.setAmount(purchaseAmount);

        webTestClient
                .mutateWith(csrf())
                .put()
                .uri(uriBuilder -> uriBuilder.path("/balance/withdraw")
                        .build())
                .body(Mono.just(request), WithdrawBalanceRequest.class)
                .exchange()
                .expectStatus().isEqualTo(422);
    }
}