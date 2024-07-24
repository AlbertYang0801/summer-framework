package com.albert.summer.context;

import java.util.List;

/**
 * @author yangjunwei
 * @date 2024/7/19
 */
public interface ApplicationContext extends AutoCloseable{

    /**
     * 是否存在指定name的Bean？
     * @param name
     * @return
     */
    boolean containsBean(String name);

    /**
     * 根据name返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
     * @param name
     * @return
     * @param <T>
     */
    <T> T getBean(String name);

    /**
     * 根据name返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
     * @param name
     * @param requiredType
     * @return
     * @param <T>
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 根据type返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
     * 如果存在多个相同类型Bean，返回@Primary标注的Bean。否则抛出异常
     * @param requiredType
     * @return
     * @param <T>
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 根据type返回一组Bean，未找到返回空List
     * @param requiredType
     * @return
     * @param <T>
     */
    <T> List<T> getBeans(Class<T> requiredType);

    /**
     * 关闭所有bean的destroy方法
     * @throws Exception
     */
    @Override
    void close();


}
