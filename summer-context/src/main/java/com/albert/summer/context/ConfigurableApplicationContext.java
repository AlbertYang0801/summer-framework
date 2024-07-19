package com.albert.summer.context;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author yangjunwei
 * @date 2024/7/19
 */
public interface ConfigurableApplicationContext extends ApplicationContext{

    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(String name);

    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    Object createBeanAsEarlySingleton(BeanDefinition def);


}
