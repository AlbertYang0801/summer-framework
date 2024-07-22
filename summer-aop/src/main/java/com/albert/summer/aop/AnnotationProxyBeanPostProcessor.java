package com.albert.summer.aop;

import com.albert.summer.context.ApplicationContextUtils;
import com.albert.summer.context.BeanDefinition;
import com.albert.summer.context.BeanPostProcessor;
import com.albert.summer.context.ConfigurableApplicationContext;
import com.albert.summer.exception.AopConfigException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP拦截实现
 *
 * @author yangjunwei
 * @date 2024/7/22
 */
public abstract class AnnotationProxyBeanPostProcessor<A extends Annotation> implements BeanPostProcessor {

    Map<String, Object> originBeans = new HashMap<>();

    /**
     * 原始类
     */
    Class<A> annotationClass;

    public AnnotationProxyBeanPostProcessor() {
        this.annotationClass = getParameterizedType();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();

        //获取符合当前拦截器的Bean，@Around，@After，@Before
        A annotation = beanClass.getAnnotation(annotationClass);

        if (annotation != null) {
            String handlerName;
            try {
                //反射获取指定注解的value
                //对应AOP的handler
                handlerName = (String) annotation.annotationType().getMethod("value").invoke(annotation);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException(String.format("@%s must have value() returned String type.", this.annotationClass.getSimpleName()), e);
            }
            Object proxy = createProxy(bean, handlerName);
            originBeans.put(beanName, bean);
            return proxy;
        } else {
            return bean;
        }
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = this.originBeans.get(beanName);
        return origin != null ? origin : bean;
    }

    /**
     * 根据handlerName查找Bean
     * 对原始Bean执行代理逻辑
     *
     * @param bean
     * @param handlerName
     * @return
     */
    Object createProxy(Object bean, String handlerName) {
        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) ApplicationContextUtils.getRequiredApplicationContext();
        BeanDefinition def = ctx.findBeanDefinition(handlerName);
        if (def == null) {
            throw new AopConfigException(String.format("@%s proxy handler '%s' not found.", this.annotationClass.getSimpleName(), handlerName));
        }
        Object handlerBean = def.getInstance();
        if (handlerBean == null) {
            handlerBean = ctx.createBeanAsEarlySingleton(def);
        }
        if (handlerBean instanceof InvocationHandler handler) {
            //调用Proxy类，执行代理逻辑
            return ProxyResolver.getInstance().createProxy(bean, handler);
        } else {
            throw new AopConfigException(String.format("@%s proxy handler '%s' is not type of %s.", this.annotationClass.getSimpleName(), handlerName,
                    InvocationHandler.class.getName()));
        }
    }

    /**
     * 获取泛型对应的Class类型
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    private Class<A> getParameterizedType() {
        //泛型
        Type type = getClass().getGenericSuperclass();
        //是否是泛型
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        ParameterizedType pt = (ParameterizedType) type;
        Type[] types = pt.getActualTypeArguments();
        //是否只有一个泛型参数
        if (types.length != 1) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " has more than 1 parameterized types.");
        }
        Type r = types[0];
        //泛型是否是一个具体的类
        if (!(r instanceof Class<?>)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<A>) r;
    }


}
