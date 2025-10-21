package ru.yandex.practicum.market.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.repository.ItemRepository;
import ru.yandex.practicum.market.dto.ItemCacheDto;

import java.time.Duration;
import java.util.Base64;


@Service
public class RedisCacheService {
    private final ReactiveRedisTemplate<String, ItemCacheDto> reactiveItemRedisTemplate;
    private final ReactiveHashOperations<String, String, ItemCacheDto> hashOps;
    private final ItemRepository itemRepository;
    private final String redisKey = "items";

    @Value("${redis.ttl}")
    private Integer ttlInMinutes;

    public RedisCacheService(ReactiveRedisTemplate<String, ItemCacheDto> reactiveItemRedisTemplate,
                             ItemRepository itemRepository) {
        this.reactiveItemRedisTemplate = reactiveItemRedisTemplate;
        this.hashOps = reactiveItemRedisTemplate.opsForHash();
        this.itemRepository = itemRepository;
    }

    public Mono<ItemCacheDto> getItemById(Long id) {
        return reactiveItemRedisTemplate.hasKey(redisKey)
                .flatMap(exists -> {
                    if (exists) {
                        return hashOps.get(redisKey, id.toString());
                    } else {
                        return loadAllItemsToCache()
                                .then(hashOps.get(redisKey, id.toString()));
                    }
                });
    }

    public Flux<ItemCacheDto> getAllItems() {
        return reactiveItemRedisTemplate.hasKey(redisKey)
                .flatMapMany(exists -> {
                    if (exists) {
                        return hashOps.values(redisKey);
                    } else {
                        return loadAllItemsToCache()
                                .thenMany(hashOps.values(redisKey));
                    }
                });
    }

    private Mono<Void> loadAllItemsToCache() {
        return itemRepository.findAll()
                .collectList()
                .flatMapMany(Flux::fromIterable)
                .flatMap(item -> hashOps.put(redisKey, item.getId().toString(), getItemCacheDto(item)))
                .then(reactiveItemRedisTemplate.expire(redisKey, Duration.ofMinutes(ttlInMinutes)))
                .then();
    }

    private ItemCacheDto getItemCacheDto(ItemEntity itemEntity) {
        ItemCacheDto newItemCacheDto = new ItemCacheDto();
        newItemCacheDto.setId(itemEntity.getId());
        newItemCacheDto.setName(itemEntity.getName());
        newItemCacheDto.setDescription(itemEntity.getDescription());
        newItemCacheDto.setImageBase64(Base64.getEncoder().encodeToString(itemEntity.getImage()));
        newItemCacheDto.setPrice(itemEntity.getPrice());
        return newItemCacheDto;
    }
}