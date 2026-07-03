package com.zerotrust.configuration;

import com.zerotrust.redis.RedisSubscriber;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(2);

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .build();
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.zerotrust.")
                .allowIfSubType("java.")
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .findAndAddModules()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJacksonJsonRedisSerializer(mapper));

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ChannelTopic biometricStreamTopic() {
        return new ChannelTopic("biometric_stream");
    }

    @Bean
    public ChannelTopic authResultsTopic() {
        return new ChannelTopic("auth_results");
    }

    @Bean
    public MessageListenerAdapter authResultListener(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleAuthResult");
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            LettuceConnectionFactory connectionFactory,
            MessageListenerAdapter authResultListener,
            ChannelTopic authResultsTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(authResultListener, authResultsTopic);

        return container;
    }
}
