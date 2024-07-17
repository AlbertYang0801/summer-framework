package com.albert.summer.bean;

import com.albert.summer.annotation.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 需要通过@Component扫描该类，并加载到Bean容器
 * @author yangjunwei
 * @date 2024/7/16
 */
@Component
public class Hello {

    /**
     * 为Bean生成BeanDefinition时，设置初始化和销毁方法
     */
    @PostConstruct
    public void init() {

    }

    @PreDestroy
    public void destroy() {

    }


}
