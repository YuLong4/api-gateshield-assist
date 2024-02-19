package com.yyl.gateshield.assist.config;


import com.yyl.gateshield.assist.application.GatewayApplication;
import com.yyl.gateshield.assist.domain.service.GatewayCenterService;
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
    public GatewayApplication gatewayApplication(GatewayServiceProperties properties, GatewayCenterService gatewayCenterService) {
        return new GatewayApplication(properties, gatewayCenterService);
    }
}
