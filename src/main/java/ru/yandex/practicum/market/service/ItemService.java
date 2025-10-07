package ru.yandex.practicum.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.repository.ItemRepository;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.OrderWithItemsDto;
import ru.yandex.practicum.market.enums.SortEnum;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ItemService {

    private final R2dbcEntityTemplate entityTemplate;
    private final ItemRepository itemRepository;


    public Mono<Page<ItemDto>> getAllItemsByConditions(Mono<OrderWithItemsDto> orderWithItemsDtoMono,
                                                       String search, String sort, int page, int pageSize) {
        Query query;
        if (search == null || search.isEmpty()) {
            query = Query.empty();
        } else {
            query = Query.query(Criteria.where("name").like("%" + search + "%"));
        }
        Pageable pageable = fillPageable(sort, page, pageSize);

        Mono<List<ItemEntity>> content = entityTemplate.select(ItemEntity.class)
                .matching(query.with(pageable))
                .all()
                .collectList();

        Mono<Long> count = entityTemplate.select(ItemEntity.class)
                .matching(query)
                .count();
        return Mono.zip(content, count, orderWithItemsDtoMono)
                .map(tuple -> {
                    List<ItemDto> convertedEntities = tuple.getT1()
                            .stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                    long totalCount = tuple.getT2();
                    OrderWithItemsDto orderDto = tuple.getT3();
                    if (orderDto.getItemList().isEmpty()) {
                        return new PageImpl<>(convertedEntities, pageable, totalCount);
                    } else {
                        enrichConvertedEntities(orderDto.getItemList(), convertedEntities);
                        return new PageImpl<>(convertedEntities, pageable, totalCount);
                    }
                });
    }

    public Mono<ItemDto> findByIdWithQuantity(Mono<OrderWithItemsDto> orderWithItemsDtoMono, Long id) {
        return orderWithItemsDtoMono.flatMap(order -> {
            Long currentOrderId = order.getId();

            return entityTemplate.getDatabaseClient().sql("""
                            SELECT i.id, i.name, i.description, i.price, i.image, oi.orders_id, oi.quantity
                            FROM items i
                            LEFT JOIN orders_items oi ON i.id = oi.items_id AND (oi.orders_id = :orderId OR oi.orders_id IS NULL)
                            WHERE i.id = :id
                            """)
                    .bind("orderId", currentOrderId)
                    .bind("id", id)
                    .fetch()
                    .all()
                    .collectList()
                    .map(rows -> {
                        if (rows.isEmpty()) {
                            return null;
                        }
                        var firstRow = rows.get(0);

                        ItemDto item = new ItemDto();
                        item.setId((Long) firstRow.get("id"));
                        item.setName((String) firstRow.get("name"));
                        item.setDescription((String) firstRow.get("description"));
                        item.setPrice((BigDecimal) firstRow.get("price"));

                        ByteBuffer buffer = (ByteBuffer) firstRow.get("image");
                        if (buffer != null && buffer.hasArray()) {
                            byte[] imageBytes = buffer.array();
                            item.setImageBase64(Base64.getEncoder().encodeToString(imageBytes));
                        }

                        // Ищем в rows запись с нужным orders_id
                        Integer quantity = 0;
                        for (var row : rows) {
                            Long ordersId = (Long) row.get("orders_id");
                            if (ordersId != null && ordersId.equals(currentOrderId)) {
                                quantity = (Integer) row.get("quantity");
                                break;
                            }
                        }
                        item.setQuantity(quantity);

                        return item;
                    });
        });
    }

    public Mono<ItemEntity> findById(Long id) {
        return itemRepository.findById(id);
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

    private ItemDto convertToDto(ItemEntity entity) {
        ItemDto item = new ItemDto();
        item.setId(entity.getId());
        item.setQuantity(0);
        item.setName(entity.getName());
        item.setDescription(entity.getDescription());
        item.setPrice(entity.getPrice());

        if (entity.getImage() != null) {
            item.setImageBase64(Base64.getEncoder().encodeToString(entity.getImage()));
        }
        return item;
    }
}