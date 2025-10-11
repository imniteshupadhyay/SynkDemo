package com.playmotech.api.core.config;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.playmotech.api.core.websockets.ScoreUpdateMessageListener;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RedisMessagingConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.ssl.enabled}")
    private boolean sslEnabled;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout}")
    private long timeout;

    @Bean
    RedisConnectionFactory redisConnectionFactory() {

        log.info("redisHost: {}", redisHost);
        log.info("redisPort: {}", redisPort);
        log.info("sslEnabled: {}", sslEnabled);
        log.info("redisUsername: {}", redisUsername);
        log.info("timeout: {} ms", timeout);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        if (StringUtils.isNotBlank(redisUsername) && StringUtils.isNotBlank(redisPassword)) {
            config.setUsername(redisUsername);
            config.setPassword(redisPassword);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration
                .builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .shutdownTimeout(Duration.ofMillis(timeout));

        if (sslEnabled) {
            log.info("Enabling SSL for Redis connection");
            clientConfig.useSsl();
        }

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofMillis(timeout))
                        .keepAlive(true)
                        .build())
                .protocolVersion(ProtocolVersion.RESP2)
                .autoReconnect(true)
                .build();

        clientConfig.clientOptions(clientOptions);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig.build());

        factory.setValidateConnection(true);
        factory.setShareNativeConnection(true);

        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    @Bean
    ChannelTopic scoreUpdatesTopic() {
        return new ChannelTopic("scores.updates");
    }

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ScoreUpdateMessageListener scoreUpdateMessageListener,
            ChannelTopic scoreUpdatesTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(scoreUpdateMessageListener, scoreUpdatesTopic);
        return container;
    }
}
