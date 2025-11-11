package ru.yandex.practicum.market.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.repository.UserRepository;
import ru.yandex.practicum.market.dto.CustomUserDetails;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUserName(username)
                .map(user -> new CustomUserDetails(
                        user.getId(),
                        user.getUserName(),
                        user.getPassword(),
                        Collections.emptyList()));
    }


    public Mono<Long> getUserIdFromPrincipal(Mono<CustomUserDetails> principal) {
        if (principal == null) {
            return Mono.just(-1L);
        }
        return principal
                .map(CustomUserDetails::getId)
                .defaultIfEmpty(-1L);
    }
}