package com.albert.summer.before;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.ComponentScan;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.aop.AroundProxyBeanPostProcessor;

/**
 * @author yjw
 * @date 2024/7/22 21:59
 */
@Configuration
@ComponentScan
public class BeforeApplication {

    @Bean
    public AroundProxyBeanPostProcessor aroundProxyBeanPostProcessor(){
        return new AroundProxyBeanPostProcessor();
    }


}
