package ru.yandex.practicum.market.dao.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.UserEntity;

@Repository
public interface UserRepository extends ReactiveCrudRepository<UserEntity, Long> {
    Mono<UserEntity> findByUserName(String username);
}