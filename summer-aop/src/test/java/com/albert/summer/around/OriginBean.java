package com.albert.summer.around;

import com.albert.summer.annotation.Around;
import com.albert.summer.annotation.Component;
import com.albert.summer.annotation.Value;

/**
 * @author yangjunwei
 * @date 2024/7/22
 */
@Component
@Around("aroundInvocationHandler")
public class OriginBean {

    @Value("${customer.name}")
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }


}
