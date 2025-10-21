package ru.yandex.practicum.market.dao.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.market.dao.entity.ItemEntity;


@Repository
public interface ItemRepository extends ReactiveCrudRepository<ItemEntity, Long> {
}