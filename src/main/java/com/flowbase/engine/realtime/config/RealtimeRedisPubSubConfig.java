package com.flowbase.engine.realtime.config;

import com.flowbase.engine.realtime.listener.RedisMessageBroadcastListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RealtimeRedisPubSubConfig {

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("flowbase-realtime-channel"));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisMessageBroadcastListener receiver) {
        return new MessageListenerAdapter(receiver, "onMessage");
    }
}
