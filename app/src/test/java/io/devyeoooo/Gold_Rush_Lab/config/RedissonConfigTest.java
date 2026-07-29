package io.devyeoooo.Gold_Rush_Lab.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class RedissonConfigTest {

    @Test
    void Redis_호스트와_포트_프로퍼티를_주입받는다() throws NoSuchFieldException {
        Field host = RedissonConfig.class.getDeclaredField("host");
        Field port = RedissonConfig.class.getDeclaredField("port");

        assertEquals(
                "${spring.data.redis.host}",
                host.getAnnotation(Value.class).value()
        );
        assertEquals(
                "${spring.data.redis.port}",
                port.getAnnotation(Value.class).value()
        );
    }

    @Test
    void Redis_주소로_Single_Server_Client를_생성한다() {
        RedissonConfig redissonConfig = new RedissonConfig();
        ReflectionTestUtils.setField(redissonConfig, "host", "redis.internal");
        ReflectionTestUtils.setField(redissonConfig, "port", "6380");
        RedissonClient redissonClient = mock(RedissonClient.class);

        try (MockedStatic<Redisson> redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(any(Config.class)))
                    .thenReturn(redissonClient);

            RedissonClient created = redissonConfig.redissonClient();

            assertEquals(redissonClient, created);
            ArgumentCaptor<Config> captor = ArgumentCaptor.forClass(Config.class);
            redisson.verify(() -> Redisson.create(captor.capture()));
            assertEquals(
                    "redis://redis.internal:6380",
                    captor.getValue().useSingleServer().getAddress()
            );
        }
    }
}
