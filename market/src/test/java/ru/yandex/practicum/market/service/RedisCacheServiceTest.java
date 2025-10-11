package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.RedisTestContainer;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.repository.ItemRepository;
import ru.yandex.practicum.market.dto.ItemCacheDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("Класс для проверки взаимодействия с редис")
public class RedisCacheServiceTest extends RedisTestContainer {
    @MockBean
    private ItemRepository itemRepository;
    @Autowired
    private RedisCacheService service;
    @Autowired
    private ReactiveRedisTemplate<String, ItemCacheDto> reactiveItemRedisTemplate;
    private final String redisKey = "items";

    @Test
    @DisplayName("Получение товара по Id из кеша")
    void getItemByIdTest() {
        Long id = 1L;
        ItemEntity itemEntity = new ItemEntity();

        itemEntity.setId(id);
        itemEntity.setName("Корм для кошек");
        itemEntity.setDescription("Корм для кошек");
        itemEntity.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        itemEntity.setImage("".getBytes());

        Flux<ItemEntity> itemEntityFlux = Flux.just(itemEntity);

        when(itemRepository.findAll()).thenReturn(itemEntityFlux);
        Mono<ItemCacheDto> itemCacheDtoMono = service.getItemById(id);
        ItemCacheDto itemCacheDto = itemCacheDtoMono.block();

        verify(itemRepository, times(1)).findAll();

        assertEquals(itemCacheDto.getId(), itemEntity.getId());
        assertEquals(itemCacheDto.getDescription(), itemEntity.getDescription());
        assertEquals(itemCacheDto.getPrice(), itemEntity.getPrice());
        assertEquals(itemCacheDto.getName(), itemEntity.getName());

        reactiveItemRedisTemplate.delete(redisKey).block();
    }

    @Test
    @DisplayName("Получение всех товаров")
    void findCartOrderAndCheckTest() {
        Long id = 1L;
        ItemEntity itemEntity = new ItemEntity();

        itemEntity.setId(id);
        itemEntity.setName("Корм для кошек");
        itemEntity.setDescription("Корм для кошек");
        itemEntity.setPrice(BigDecimal.valueOf(105).setScale(2, RoundingMode.HALF_UP));
        itemEntity.setImage("".getBytes());

        Flux<ItemEntity> itemEntityFlux = Flux.just(itemEntity);

        when(itemRepository.findAll()).thenReturn(itemEntityFlux);
        Flux<ItemCacheDto> itemCacheDtoFlux = service.getAllItems();
        ItemCacheDto itemCacheDto = itemCacheDtoFlux.blockFirst();

        verify(itemRepository, times(1)).findAll();

        assertEquals(itemCacheDto.getId(), itemEntity.getId());
        assertEquals(itemCacheDto.getDescription(), itemEntity.getDescription());
        assertEquals(itemCacheDto.getPrice(), itemEntity.getPrice());
        assertEquals(itemCacheDto.getName(), itemEntity.getName());

        reactiveItemRedisTemplate.delete(redisKey).block();
    }
}