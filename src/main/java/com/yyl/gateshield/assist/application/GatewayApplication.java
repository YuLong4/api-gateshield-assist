package com.yyl.gateshield.assist.application;

import com.alibaba.fastjson.JSON;
import com.yyl.gateshield.assist.config.GatewayServiceProperties;
import com.yyl.gateshield.assist.domain.model.aggregates.ApplicationSystemRichInfo;
import com.yyl.gateshield.assist.domain.model.vo.ApplicationInterfaceMethodVO;
import com.yyl.gateshield.assist.domain.model.vo.ApplicationInterfaceVO;
import com.yyl.gateshield.assist.domain.model.vo.ApplicationSystemVO;
import com.yyl.gateshield.assist.domain.service.GatewayCenterService;
import com.yyl.gateshield.core.mapping.HttpCommandType;
import com.yyl.gateshield.core.mapping.HttpStatement;
import com.yyl.gateshield.core.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;

/**
 * 网关应用；与 Spring 链接，调用网关注册和接口拉取
 */
public class GatewayApplication implements ApplicationListener<ContextRefreshedEvent> {

    private Logger logger = LoggerFactory.getLogger(GatewayApplication.class);

    private GatewayServiceProperties properties;
    private GatewayCenterService gatewayCenterService;
    private Configuration configuration;

    public GatewayApplication(GatewayServiceProperties properties, GatewayCenterService gatewayCenterService, Configuration configuration) {
        this.properties = properties;
        this.gatewayCenterService = gatewayCenterService;
        this.configuration = configuration;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 1. 注册网关服务；每一个用于转换 HTTP 协议泛化调用到 RPC 接口的网关都是一个算力，这些算力需要注册网关配置中心
        gatewayCenterService.doRegister(properties.getAddress(),
                properties.getGroupId(),
                properties.getGatewayId(),
                properties.getGatewayName(),
                properties.getGatewayAddress());

        // 2. 拉取网关配置；每个网关算力都会在注册中心分配上需要映射的RPC服务信息，包括；系统、接口、方法
        ApplicationSystemRichInfo applicationSystemRichInfo = gatewayCenterService.pullApplicationSystemRichInfo(properties.getAddress(), properties.getGatewayId());
        logger.info("拉取的网关配置: " + JSON.toJSONString(applicationSystemRichInfo));
        List<ApplicationSystemVO> applicationSystemVOList = applicationSystemRichInfo.getApplicationSystemVOList();
        for (ApplicationSystemVO system : applicationSystemVOList) {
            List<ApplicationInterfaceVO> interfaceList = system.getInterfaceList();
            for (ApplicationInterfaceVO itf : interfaceList) {
                // 2.1 创建配置信息加载注册
                logger.info("执行2.1 创建配置信息加载注册,registryConfig参数为:" + system.getSystemId() +" "+ system.getSystemRegistry() + " " + itf.getInterfaceId()+" " + itf.getInterfaceVersion());
                configuration.registryConfig(system.getSystemId(), system.getSystemRegistry(), itf.getInterfaceId(), itf.getInterfaceVersion());
                List<ApplicationInterfaceMethodVO> methodList = itf.getMethodList();
                // 2.2 注册系统服务接口信息
                for (ApplicationInterfaceMethodVO method : methodList) {
                    HttpStatement httpStatement = new HttpStatement(
                            system.getSystemId(),
                            itf.getInterfaceId(),
                            method.getMethodId(),
                            method.getParameterType(),
                            method.getUri(),
                            HttpCommandType.valueOf(method.getHttpCommandType()),
                            method.isAuth());
                    configuration.addMapper(httpStatement);
                    logger.info("网关服务注册映射 系统：{} 接口：{} 方法：{}", system.getSystemId(), itf.getInterfaceId(), method.getMethodId());
                }
            }
        }
    }
}
