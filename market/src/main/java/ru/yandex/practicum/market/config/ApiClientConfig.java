package ru.yandex.practicum.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.market.ApiClient;
import ru.yandex.practicum.market.api.BalanceApi;

@Configuration
public class ApiClientConfig {

    @Bean
    public BalanceApi balanceApi() {
        return new BalanceApi(apiClient());
    }

    @Bean
    public ApiClient apiClient() {
        return new ApiClient();
    }
}