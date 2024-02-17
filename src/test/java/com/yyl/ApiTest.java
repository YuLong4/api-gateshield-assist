package com.yyl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.yyl.gateshield.assist.common.Result;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ApiTest {

    @Test
    public void test_register_gateway(){
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("groupId", "10001");
        paramMap.put("gatewayId", "api-gateway-g4");
        paramMap.put("gatewayName", "电商配送网关");
        paramMap.put("gatewayAddress", "127.0.0.1");

        String resultStr = HttpUtil.post("http://localhost/wg/admin/config/registerGateway", paramMap);
        System.out.println(resultStr);

        Result result = JSON.parseObject(resultStr, Result.class);
        System.out.println(result.getCode());
    }
}