package com.albert.summer.aop;/**
 * @author yjw
 * @date 2024/7/22 21:11
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 适配器模式
 * 基于@Before注解
 * @author Albert.Yang
 * @date 2024/7/22
 */
public abstract class BeforeInvocationHandlerAdapter implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before(proxy, method, args);
        return method.invoke(proxy, args);
    }

    public abstract void before(Object proxy, Method method, Object[] args) throws Throwable;


}
