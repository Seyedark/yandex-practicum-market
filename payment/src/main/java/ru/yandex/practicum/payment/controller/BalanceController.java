package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.BalanceApi;
import ru.yandex.practicum.payment.domain.Balance;
import ru.yandex.practicum.payment.domain.WithdrawBalanceRequest;
import ru.yandex.practicum.payment.exception.InsufficientBalanceException;
import ru.yandex.practicum.payment.service.PaymentService;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class BalanceController implements BalanceApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<Balance>> getBalance(BigDecimal purchaseAmount, ServerWebExchange exchange) {
        return paymentService.getBalance(purchaseAmount)
                .map(ResponseEntity::ok)
                .onErrorResume(InsufficientBalanceException.class,
                        e -> Mono.just(ResponseEntity.badRequest().body(e.getBalance())));
    }

    @Override
    public Mono<ResponseEntity<Balance>> withdrawBalance(Mono<WithdrawBalanceRequest> withdrawBalanceRequest, ServerWebExchange exchange) {
        return paymentService.withdrawBalance(withdrawBalanceRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(InsufficientBalanceException.class,
                        e -> Mono.just(ResponseEntity.badRequest().body(e.getBalance())));
    }
}