package com.albert.summer.after;

import com.albert.summer.annotation.Component;
import com.albert.summer.aop.AfterInvocationHandlerAdapter;

import java.lang.reflect.Method;

/**
 * 拦截器适配器
 * 适配@After逻辑
 * @author yjw
 * @date 2024/7/22 21:55
 */
@Component
public class PoliteInvocationHandler extends AfterInvocationHandlerAdapter {

    /**
     * 方法之后执行
     * @Around 对应的拦截器，修改为代理对象后执行after方法
     * @param proxy
     * @param returnValue
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) throws Throwable {
        if (returnValue instanceof String s) {
            if (s.endsWith(".")) {
                return s.substring(0, s.length() - 1) + "!";
            }
        }
        return returnValue;
    }


}
