package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.repository.ItemRepository;
import ru.yandex.practicum.market.dto.ItemCacheDto;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.dto.PageResponseDto;
import ru.yandex.practicum.market.enums.SortEnum;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final RedisCacheService redisCacheService;

    public Mono<PageResponseDto> getAllItemsByConditions(Mono<OrderWithItemsDto> orderWithItemsDtoMono,
                                                         String search, String sort, long page, long pageSize) {
        return orderWithItemsDtoMono
                .flatMap(orderWithItemsDto -> redisCacheService
                        .getAllItems()
                        .collectList()
                        .map(list -> {
                            List<ItemCacheDto> filteredSortedList = sortList(list, search, sort);
                            long totalPages = (long) Math.ceil((double) filteredSortedList.size() / pageSize);
                            List<ItemDto> convertedItemList = filteredSortedList
                                    .stream()
                                    .skip((page - 1) * pageSize)
                                    .limit(pageSize)
                                    .map(this::convertToDto)
                                    .toList();
                            PageResponseDto pageResponseDto = new PageResponseDto();
                            pageResponseDto.setTotalPages(totalPages);
                            if (!orderWithItemsDto.getItemList().isEmpty()) {
                                enrichConvertedEntities(orderWithItemsDto.getItemList(), convertedItemList);
                            }
                            pageResponseDto.setItemDtoList(convertedItemList);
                            return pageResponseDto;
                        }));
    }

    public Mono<ItemDto> findByIdWithQuantity(Mono<OrderWithItemsDto> orderWithItemsDtoMono, Long id) {
        return orderWithItemsDtoMono
                .flatMap(order -> redisCacheService.getItemById(id)
                        .map(itemCacheDto -> {
                            ItemDto itemDto = new ItemDto();
                            itemDto.setId(itemCacheDto.getId());
                            itemDto.setName(itemCacheDto.getName());
                            itemDto.setDescription(itemCacheDto.getDescription());
                            itemDto.setImageBase64(itemCacheDto.getImageBase64());
                            itemDto.setPrice(itemCacheDto.getPrice());
                            itemDto.setQuantity(getQuantityFromItemList(order.getItemList(), id));
                            return itemDto;
                        }));
    }

    private Integer getQuantityFromItemList(List<ItemDto> itemDtoList, Long id) {
        return itemDtoList.stream()
                .filter(x -> x.getId().equals(id))
                .findFirst()
                .map(ItemDto::getQuantity)
                .orElse(0);
    }


    public Mono<ItemEntity> findById(Long id) {
        return itemRepository.findById(id);
    }

    private List<ItemCacheDto> sortList(List<ItemCacheDto> itemCacheDtoList, String search, String sort) {
        if (SortEnum.NAME.name().equals(sort)) {
            Comparator<ItemCacheDto> comparator = Comparator.comparing(ItemCacheDto::getName);
            return itemCacheDtoList.stream()
                    .sorted(comparator)
                    .filter(x -> search == null || x.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        } else if (SortEnum.PRICE.name().equals(sort)) {
            Comparator<ItemCacheDto> comparator = Comparator.comparing(ItemCacheDto::getPrice);
            return itemCacheDtoList.stream()
                    .sorted(comparator)
                    .filter(x -> search == null || x.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            return itemCacheDtoList.stream()
                    .filter(x -> search == null || x.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }
    }

    private void enrichConvertedEntities(List<ItemDto> orderItemList, List<ItemDto> convertedEntities) {

        Map<Long, Integer> idToQuantity = orderItemList.stream()
                .collect(Collectors.toMap(ItemDto::getId, ItemDto::getQuantity));

        convertedEntities.forEach(item -> {
            Integer quantity = idToQuantity.get(item.getId());
            if (quantity != null) {
                item.setQuantity(quantity);
            }
        });
    }

    private ItemDto convertToDto(ItemCacheDto itemCacheDto) {
        ItemDto item = new ItemDto();
        item.setId(itemCacheDto.getId());
        item.setQuantity(0);
        item.setName(itemCacheDto.getName());
        item.setDescription(itemCacheDto.getDescription());
        item.setPrice(itemCacheDto.getPrice());
        item.setImageBase64(itemCacheDto.getImageBase64());
        return item;
    }
}