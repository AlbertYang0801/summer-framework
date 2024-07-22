package com.albert.summer.aop;/**
 * @author yjw
 * @date 2024/7/22 21:11
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 适配器模式
 * 基于@After注解
 * @author Albert.Yang
 * @date 2024/7/22
 */
public abstract class AfterInvocationHandlerAdapter implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object invoke = method.invoke(proxy, args);
        return after(proxy, invoke, method, args);
    }

    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args) throws Throwable;


}
