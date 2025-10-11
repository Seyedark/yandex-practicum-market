package ru.yandex.practicum.market.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.api.BalanceApi;
import ru.yandex.practicum.market.domain.Balance;
import ru.yandex.practicum.market.domain.WithdrawBalanceRequest;
import ru.yandex.practicum.market.dto.BalanceApiResponseDto;
import ru.yandex.practicum.market.enums.BalanceApiEnum;

import java.math.BigDecimal;

@Service
public class PaymentApiService extends BalanceApi {

    public Mono<BalanceApiResponseDto> formBalanceResponse(BigDecimal totalAmount, boolean check) {
        if (check) {
            return getBalanceWithHttpInfo(totalAmount)
                    .flatMap(x -> formSuccessResponse())
                    .onErrorResume(this::formErrorResponse);
        } else {
            WithdrawBalanceRequest withdrawBalanceRequest = new WithdrawBalanceRequest();
            return withdrawBalance(withdrawBalanceRequest.amount(totalAmount))
                    .flatMap(x -> formSuccessResponse())
                    .onErrorResume(this::formErrorResponse);
        }
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