package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.domain.Balance;
import ru.yandex.practicum.payment.domain.WithdrawBalanceRequest;

import java.math.BigDecimal;

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
    @DisplayName("Проверка метода получения баланса с успешным статусом")
    void getBalanceWithSuccessResponseTest() {
        BigDecimal purchaseAmount = BigDecimal.valueOf(100);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/balance")
                        .queryParam("purchaseAmount", purchaseAmount)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Balance.class);
    }

    @Test
    @DisplayName("Проверка метода получения баланса с ошибочным статусом")
    void getBalanceWithErrorResponseTest() {
        BigDecimal purchaseAmount = BigDecimal.valueOf(10000);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/balance")
                        .queryParam("purchaseAmount", purchaseAmount)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Balance.class);
    }

    @Test
    @DisplayName("Проверка метода списания с успешным статусом")
    void withdrawBalanceWithSuccessResponseTest() {
        WithdrawBalanceRequest request = new WithdrawBalanceRequest();
        BigDecimal purchaseAmount = BigDecimal.valueOf(100);
        request.setAmount(purchaseAmount);

        webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path("/balance/withdraw")
                        .build())
                .body(Mono.just(request), WithdrawBalanceRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Balance.class)
                .value(balance -> Assertions.assertEquals(balance.getAmount(), amount.subtract(purchaseAmount)));
    }

    @Test
    @DisplayName("Проверка метода списания с ошибочным статусом")
    void withdrawBalanceWithErrorResponseTest() {
        WithdrawBalanceRequest request = new WithdrawBalanceRequest();
        BigDecimal purchaseAmount = BigDecimal.valueOf(10000);
        request.setAmount(purchaseAmount);

        webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path("/balance/withdraw")
                        .build())
                .body(Mono.just(request), WithdrawBalanceRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Balance.class)
                .value(balance -> Assertions.assertEquals(balance.getAmount(), amount));
    }
}