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
import ru.yandex.practicum.payment.exception.UserNotFoundException;
import ru.yandex.practicum.payment.service.PaymentService;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class BalanceController implements BalanceApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<Balance>> getBalance(BigDecimal purchaseAmount, Long userId, ServerWebExchange exchange) {
        return paymentService.getBalance(purchaseAmount, userId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    if (e instanceof InsufficientBalanceException) {
                        return Mono.just(ResponseEntity.badRequest().body(((InsufficientBalanceException) e).getBalance()));
                    } else if (e instanceof UserNotFoundException) {
                        return Mono.just(ResponseEntity.unprocessableEntity().build());
                    }
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<ResponseEntity<Balance>> withdrawBalance(Mono<WithdrawBalanceRequest> withdrawBalanceRequest, ServerWebExchange exchange) {
        return paymentService.withdrawBalance(withdrawBalanceRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    if (e instanceof InsufficientBalanceException) {
                        return Mono.just(ResponseEntity.badRequest().body(((InsufficientBalanceException) e).getBalance()));
                    } else if (e instanceof UserNotFoundException) {
                        return Mono.just(ResponseEntity.unprocessableEntity().build());
                    }
                    return Mono.error(e);
                });
    }
}