package ru.yandex.practicum.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import ru.yandex.practicum.market.dao.repository.UserRepository;
import ru.yandex.practicum.market.service.CustomUserDetailsService;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/item").permitAll()
                        .anyExchange().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .build();
    }

    @Bean
    @Primary
    public ReactiveUserDetailsService reactiveUserDetailsService(UserRepository userRepository) {
        return new CustomUserDetailsService(userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}