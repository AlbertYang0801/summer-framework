package com.albert.summer.around;

import com.albert.summer.annotation.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 自定义业务拦截器
 *
 * @author Albert.Yang
 * @date 2024/7/22
 */
@Component
public class AroundInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getAnnotation(Polite.class) != null) {
            String ret = (String) method.invoke(proxy, args);
            //自定义增强逻辑
            if (ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1) + "!";
            }
            return ret;
        }
        return method.invoke(proxy, args);
    }


}
