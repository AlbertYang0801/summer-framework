package com.albert.summer.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Create proxy by subclassing and override methods with interceptor.
 */
@Slf4j
public class ProxyResolver {

    final ByteBuddy byteBuddy = new ByteBuddy();

    private ProxyResolver() {
    }

    public static ProxyResolver getInstance() {
        return ProxyResolverInstance.INSTANCE;
    }

    /**
     * 静态内部类实现单例
     */
    public static class ProxyResolverInstance {

        private final static ProxyResolver INSTANCE = new ProxyResolver();

    }

    /**
     * 为原始Bean，使用ByteBuddy动态代理生成代理对象
     * @param bean 原始对象
     * @param handler 字节处理器
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler) {
        Class<?> targetClass = bean.getClass();
        log.debug("create proxy for bean {} @{}", targetClass.getName(), Integer.toHexString(bean.hashCode()));
        Class<?> proxyClass = this.byteBuddy
                // 默认采用无参构造
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // 拦截所有public方法
                .method(ElementMatchers.isPublic()).intercept(InvocationHandlerAdapter.of(
                        //调用自定义拦截器实现代理细节
                        (proxy, method, args) -> {
                            // delegate to origin bean:
                            return handler.invoke(bean, method, args);
                        }))
                // generate proxy class:
                //生成字节码
                .make()
                //加载字节码
                .load(targetClass.getClassLoader()).getLoaded();
        Object proxy;
        try {
            //动态代理
            //根据字节码生成对应类的子类，实现代理功能
            proxy = proxyClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) proxy;
    }


}
