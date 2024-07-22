package com.albert.summer.after;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.ComponentScan;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.aop.AroundProxyBeanPostProcessor;

/**
 * @author yjw
 * @date 2024/7/22 21:53
 */
@ComponentScan
@Configuration
public class AfterApplication {

    @Bean
    AroundProxyBeanPostProcessor aroundProxyBeanPostProcessor(){
        return new AroundProxyBeanPostProcessor();
    }


}
