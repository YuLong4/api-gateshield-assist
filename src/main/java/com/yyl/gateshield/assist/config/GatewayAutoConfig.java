package com.yyl.gateshield.assist.config;


import com.yyl.gateshield.assist.application.GatewayApplication;
import com.yyl.gateshield.assist.domain.service.GatewayCenterService;
import com.yyl.gateshield.core.session.defaults.DefaultGatewaySessionFactory;
import com.yyl.gateshield.core.socket.GatewaySocketServer;
import io.netty.channel.Channel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 网关服务配置
 */
@Configuration
@EnableConfigurationProperties(GatewayServiceProperties.class)
public class GatewayAutoConfig {

    private Logger logger = LoggerFactory.getLogger(GatewayAutoConfig.class);

    @Bean
    public GatewayCenterService registerGatewayService(){
        return new GatewayCenterService();
    }

    @Bean
    public GatewayApplication gatewayApplication(GatewayServiceProperties properties, GatewayCenterService gatewayCenterService, com.yyl.gateshield.core.session.Configuration configuration) {
        return new GatewayApplication(properties, gatewayCenterService, configuration);
    }

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
    @Bean
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
