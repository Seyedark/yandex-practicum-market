package ru.yandex.practicum.market;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class RedisTestContainer {

    private static RedisContainer redisContainer;

    static {
        redisContainer = new RedisContainer("redis:7.4.2-bookworm");
        redisContainer.start();
    }
}
