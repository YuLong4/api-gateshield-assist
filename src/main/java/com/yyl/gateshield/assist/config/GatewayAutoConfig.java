package com.yyl.gateshield.assist.config;


import com.yyl.gateshield.assist.application.GatewayApplication;
import com.yyl.gateshield.assist.domain.service.GatewayCenterService;
import com.yyl.gateshield.core.session.defaults.DefaultGatewaySessionFactory;
import com.yyl.gateshield.core.socket.GatewaySocketServer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import redis.clients.jedis.JedisPoolConfig;


/**
 * 网关服务配置
 */
@Configuration
@EnableConfigurationProperties(GatewayServiceProperties.class)
public class GatewayAutoConfig {

    private Logger logger = LoggerFactory.getLogger(GatewayAutoConfig.class);

    @Bean
    public RedisConnectionFactory redisConnectionFactory(GatewayServiceProperties properties, GatewayCenterService gatewayCenterService) {
        //1.拉取注册中心的 Redis 配置信息
        Map<String, String> redisConfig = gatewayCenterService.queryRedisConfig(properties.getAddress());
        //2.构建Redis服务
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(redisConfig.get("host"));
        standaloneConfig.setPort(Integer.parseInt(redisConfig.get("port")));
        //3.默认配置信息
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxWaitMillis(30 * 1000);
        poolConfig.setMinIdle(20);
        poolConfig.setMaxIdle(40);
        poolConfig.setTestWhileIdle(true);
        //4.创建 Redis 配置
        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofSeconds(2)).clientName("api-gateway-assist-redis-" + properties.getGatewayId()).usePooling().poolConfig(poolConfig).build();
        //5.实例化 Redis 对象
        return new JedisConnectionFactory(standaloneConfig, clientConfig);
    }

    @Bean
    public RedisMessageListenerContainer container(GatewayServiceProperties properties, RedisConnectionFactory redisConnectionFactory, MessageListenerAdapter messageListenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic(properties.getGatewayId()));
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(GatewayApplication gatewayApplication) {
        return new MessageListenerAdapter(gatewayApplication, "receiveMessage");
    }

    @Bean
    public GatewayCenterService registerGatewayService(){
        return new GatewayCenterService();
    }

    @Bean
    public GatewayApplication gatewayApplication(GatewayServiceProperties properties, GatewayCenterService gatewayCenterService, com.yyl.gateshield.core.session.Configuration configuration, Channel gatewaySocketServerChannel) {
        return new GatewayApplication(properties, gatewayCenterService, configuration, gatewaySocketServerChannel);
    }

    //创建网关配置对象
    @Bean
    public com.yyl.gateshield.core.session.Configuration gatewayCoreConfiguration(GatewayServiceProperties properties) {
        com.yyl.gateshield.core.session.Configuration configuration = new com.yyl.gateshield.core.session.Configuration();
        String[] split = properties.getGatewayAddress().split(":");
        configuration.setHostName(split[0].trim());
        configuration.setPort(Integer.parseInt(split[1].trim()));
        return configuration;
    }

    /**
     * 初始化网关服务 创建服务端 Channel 对象，方便获取和控制网关操作。
     */
    @Bean("gatewaySocketServerChannel")
    public Channel initGateway(com.yyl.gateshield.core.session.Configuration configuration) throws ExecutionException, InterruptedException {
        //1.基于配置构建会话工厂
        DefaultGatewaySessionFactory gatewaySessionFactory = new DefaultGatewaySessionFactory(configuration);
        //2.创建启动网关网络服务
        GatewaySocketServer server = new GatewaySocketServer(configuration, gatewaySessionFactory);
        Future<Channel> future = Executors.newFixedThreadPool(2).submit(server);
        Channel channel = future.get();
        if (channel == null) {
            throw new RuntimeException("api gateshield core netty server start error,  channel is null");
        }
        while (!channel.isActive()) {
            logger.info("api gateshield core netty server gateway starting...");
            Thread.sleep(500);
        }
        logger.info("api gateshield core netty server gateway start Done! {}", channel.localAddress());
        return channel;
    }
}
