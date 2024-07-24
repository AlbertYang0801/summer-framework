package com.albert.summer.tx;

import com.albert.summer.annotation.Transactional;
import com.albert.summer.aop.AnnotationProxyBeanPostProcessor;

/**
 * 基于AOP的BeanPostProcessor实现拦截Transactional注解
 * @author yjw
 * @date 2024/7/23 21:09
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {



}
