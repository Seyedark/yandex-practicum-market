package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.api.BalanceApi;
import ru.yandex.practicum.market.domain.Balance;
import ru.yandex.practicum.market.domain.WithdrawBalanceRequest;
import ru.yandex.practicum.market.dto.BalanceApiResponseDto;
import ru.yandex.practicum.market.enums.BalanceApiEnum;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApiService {
    private final ReactiveOAuth2AuthorizedClientManager manager;
    private final BalanceApi balanceApi;

    public Mono<BalanceApiResponseDto> formBalanceResponse(BigDecimal totalAmount, Long userId, boolean check) {
        return manager.authorize(OAuth2AuthorizeRequest
                        .withClientRegistrationId("yandex")
                        .principal("system")
                        .build())
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue)
                .flatMap(accessToken -> {
                    System.out.println(accessToken);
                    balanceApi.getApiClient().addDefaultHeader("Authorization", "Bearer " + accessToken);
                    if (check) {
                        return balanceApi
                                .getBalanceWithHttpInfo(totalAmount, userId)
                                .flatMap(x -> formSuccessResponse());
                    } else {
                        WithdrawBalanceRequest withdrawBalanceRequest = new WithdrawBalanceRequest();
                        return balanceApi.withdrawBalance(withdrawBalanceRequest.amount(totalAmount).userId(userId))
                                .flatMap(x -> formSuccessResponse());
                    }
                })
                .onErrorResume(this::formErrorResponse);
    }

    private Mono<BalanceApiResponseDto> formSuccessResponse() {
        BalanceApiResponseDto balanceApiResponseDto = new BalanceApiResponseDto();
        balanceApiResponseDto.setCode(BalanceApiEnum.SUCCESS.getCode());
        return Mono.just(balanceApiResponseDto);
    }

    private Mono<BalanceApiResponseDto> formErrorResponse(Throwable throwable) {
        BalanceApiResponseDto dto = new BalanceApiResponseDto();
        if (throwable instanceof WebClientResponseException e) {
            int statusCode = e.getRawStatusCode();
            if (BalanceApiEnum.INSUFFICIENT_BALANCE_ERROR.getCode().equals(statusCode)) {
                dto.setCode(BalanceApiEnum.INSUFFICIENT_BALANCE_ERROR.getCode());
                dto.setErrorMessage(BalanceApiEnum.INSUFFICIENT_BALANCE_ERROR.getErrorMessage() + e.getResponseBodyAs(Balance.class).getAmount());
            } else if (BalanceApiEnum.USER_NOT_FOUND_ERROR.getCode().equals(statusCode)) {
                dto.setCode(BalanceApiEnum.USER_NOT_FOUND_ERROR.getCode());
                dto.setErrorMessage(BalanceApiEnum.USER_NOT_FOUND_ERROR.getErrorMessage());
            } else {
                dto.setCode(BalanceApiEnum.UNEXPECTED_ERROR.getCode());
                dto.setErrorMessage(BalanceApiEnum.UNEXPECTED_ERROR.getErrorMessage());
            }
        } else {
            dto.setCode(BalanceApiEnum.UNEXPECTED_ERROR.getCode());
            dto.setErrorMessage(BalanceApiEnum.UNEXPECTED_ERROR.getErrorMessage());
        }
        return Mono.just(dto);
    }
}