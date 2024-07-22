package com.albert.summer.around;

import com.albert.summer.context.AnnotationConfigApplicationContext;
import com.albert.summer.property.PropertyResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * @author yjw
 * @date 2024/7/22 21:19
 */
public class AroundProxyTest {

    @Test
    public void testAroundProxy() throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext(AroundApplication.class, createPropertyResolver())) {
            OriginBean proxyBean = ctx.getBean(OriginBean.class);
            //代理类被篡改
            Assertions.assertNotSame(OriginBean.class,proxyBean.getClass());
            //代理类的属性为空
            Assertions.assertNull(proxyBean.name);
            //代理逻辑执行后
            Assertions.assertEquals("Hello, Bob!", proxyBean.hello());

            //未加业务注解，不执行代理逻辑
            Assertions.assertNotEquals("Morning, Bob!", proxyBean.morning());

            OtherBean otherBean = ctx.getBean(OtherBean.class);
            //是否注入的是代理类
            Assertions.assertSame(proxyBean,otherBean.origin);
            Assertions.assertEquals("Hello, Bob!",otherBean.origin.hello());
        }


    }

    /**
     * 属性解析器
     * @return
     */
    PropertyResolver createPropertyResolver() {
        var ps = new Properties();
        ps.put("customer.name", "Bob");
        return new PropertyResolver(ps);
    }


}
