package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.dao.repository.ItemRepository;
import ru.yandex.practicum.market.enums.SortEnum;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;


    public Page<ItemEntity> getAllItemsByConditions(OrderEntity orderEntity, String search, String sort, int page, int pageSize) {
        return itemRepository.getAllItemsByConditions(orderEntity.getId(), search, fillPageable(sort, page, pageSize));
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

    public ItemEntity findById(Long id) {
        return itemRepository.findById(id).get();
    }
}