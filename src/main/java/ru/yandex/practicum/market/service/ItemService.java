package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.entity.OrderItemEntity;
import ru.yandex.practicum.market.dao.repository.ItemRepository;
import ru.yandex.practicum.market.enums.SortEnum;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;


    public Page<ItemEntity> getAllItemsByConditions(OrderEntity orderEntity, String search, String sort, int page, int pageSize) {
        List<OrderItemEntity> orderItemEntityList = orderEntity.getOrderItem();
        if (orderEntity.getOrderItem().isEmpty()) {
            return itemRepository.getAllItemsByConditionsForNewOrder(search, fillPageable(sort, page, pageSize));
        } else {
            Page<ItemEntity> itemEntityPage = itemRepository.getAllItemsByConditions(search, fillPageable(sort, page, pageSize));
            return enrichItemEntityPage(orderItemEntityList, itemEntityPage);
        }
    }

    public ItemEntity findById(Long id) {
        return itemRepository.findById(id).get();
    }

    private Pageable fillPageable(String sort, int page, int pageSize) {
        if (sort.equals(SortEnum.NO.name())) {
            return PageRequest.of(page - 1, pageSize);
        } else {
            String sortValue = sort.equals(SortEnum.NAME.name()) ?
                    SortEnum.NAME.name().toLowerCase() : SortEnum.PRICE.name().toLowerCase();
            Sort pageSort = Sort.by(sortValue).ascending();
            return PageRequest.of(page - 1, pageSize, pageSort);
        }
    }

    private Page<ItemEntity> enrichItemEntityPage(List<OrderItemEntity> orderItemEntityList, Page<ItemEntity> itemEntityPage) {
        Map<Long, Integer> quantitiesByItemId = orderItemEntityList.stream()
                .collect(Collectors
                        .toMap(orderItem -> orderItem.getItem().getId(), OrderItemEntity::getQuantity, (q1, q2) -> q1));

        List<ItemEntity> enrichedContent = itemEntityPage.getContent().stream()
                .map(item -> {
                    Integer quantity = quantitiesByItemId.get(item.getId());
                    item.setQuantity(Objects.requireNonNullElse(quantity, 0));
                    return item;
                })
                .toList();

        return new PageImpl<>(enrichedContent, itemEntityPage.getPageable(), itemEntityPage.getTotalElements());
    }
}