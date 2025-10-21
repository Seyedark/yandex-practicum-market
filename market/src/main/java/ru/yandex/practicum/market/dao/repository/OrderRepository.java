package ru.yandex.practicum.market.dao.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.market.dao.entity.OrderEntity;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<OrderEntity, Long> {
}