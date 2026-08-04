package io.devyeoooo.Gold_Rush_Lab.config;

import io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine.MiningCompletedSubscriber;
import io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine.SseCleanupSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

    public static final String MINING_COMPLETED_CHANNEL =
            "gold-rush:mining:completed";
    public static final String SSE_CLEANUP_CHANNEL =
            "gold-rush:sse:cleanup";

    @Bean
    public ChannelTopic miningCompletedTopic() {
        return new ChannelTopic(MINING_COMPLETED_CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MiningCompletedSubscriber subscriber,
            SseCleanupSubscriber cleanupSubscriber,
            ChannelTopic miningCompletedTopic
    ) {
        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, miningCompletedTopic);
        container.addMessageListener(
                cleanupSubscriber,
                new ChannelTopic(SSE_CLEANUP_CHANNEL)
        );

        return container;
    }
}
