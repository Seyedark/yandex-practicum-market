package ru.yandex.practicum.market.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.market.dao.entity.ItemEntity;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, Long> {

    @Query("SELECT new ru.yandex.practicum.market.dao.entity.ItemEntity(i.id, i.name, i.description, i.price, i.image, COALESCE(oi.quantity, 0)) FROM ItemEntity i " +
            "LEFT JOIN OrderItemEntity oi on i.id = oi.item.id " +
            "LEFT JOIN OrderEntity o ON oi.order.id = (:orderId) " +
            "WHERE (:name is null or :name = '' or i.name ilike %:name%)")
    Page<ItemEntity> getAllItemsByConditions(@Param("orderId")Long orderId, @Param("name") String name, Pageable pageable);
}