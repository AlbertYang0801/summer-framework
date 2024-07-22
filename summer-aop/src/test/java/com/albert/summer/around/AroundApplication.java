package com.albert.summer.around;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.ComponentScan;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.aop.AroundProxyBeanPostProcessor;

/**
 * @author yangjunwei
 * @date 2024/7/22
 */
@Configuration
@ComponentScan
public class AroundApplication {

    /**
     * 注入扫描@Around注解的BeanPostProcessor
     * @return
     */
    @Bean
    AroundProxyBeanPostProcessor aroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }


}
