package com.yyl.gateshield.assist.application;

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
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // 1. 注册网关服务；每一个用于转换 HTTP 协议泛化调用到 RPC 接口的网关都是一个算力，这些算力需要注册网关配置中心
        gatewayCenterService.doRegister(properties.getGatewayAddress(),
                properties.getGroupId(),
                properties.getGatewayId(),
                properties.getGatewayName(),
                properties.getGatewayAddress());
        // 2. 拉取网关配置；每个网关算力都会在注册中心分配上需要映射的RPC服务信息，包括；系统、接口、方法
        ApplicationSystemRichInfo applicationSystemRichInfo = gatewayCenterService.pullApplicationSystemRichInfo(properties.getAddress(), properties.getGatewayId());
        List<ApplicationSystemVO> applicationSystemVOList = applicationSystemRichInfo.getApplicationSystemVOList();
        for (ApplicationSystemVO system : applicationSystemVOList) {
            List<ApplicationInterfaceVO> interfaceVOList = system.getInterfaceList();
            for (ApplicationInterfaceVO interfaceVO : interfaceVOList) {
                //2.1 创建配置信息加载注册
                configuration.registryConfig(system.getSystemId(), system.getSystemRegistry(), interfaceVO.getInterfaceId(), interfaceVO.getInterfaceVersion());
                List<ApplicationInterfaceMethodVO> methodVOList = interfaceVO.getMethodList();
                //2.1注册系统服务接口信息
                for (ApplicationInterfaceMethodVO methodVO : methodVOList) {
                    HttpStatement httpStatement = new HttpStatement(
                            system.getSystemId(),
                            interfaceVO.getInterfaceId(),
                            methodVO.getMethodId(),
                            methodVO.getParameterType(),
                            methodVO.getUri(),
                            HttpCommandType.valueOf(methodVO.getHttpCommandType()),
                            methodVO.getAuth()
                    );
                    configuration.addMapper(httpStatement);
                    logger.info("网关服务注册映射 系统：{} 接口：{} 方法：{}", system.getSystemId(), interfaceVO.getInterfaceId(), methodVO.getMethodId());
                }
            }
        }
    }
}
