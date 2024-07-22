package com.albert.summer.before;

import com.albert.summer.context.AnnotationConfigApplicationContext;
import com.albert.summer.property.PropertyResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

/**
 * @author yjw
 * @date 2024/7/22 22:03
 */
public class BeforeProxyTest {

    @Test
    public void testBeforeProxy() throws IOException {
        try (var ctx = new AnnotationConfigApplicationContext(BeforeApplication.class, createPropertyResolver())) {
            BusinessBean proxyBean = ctx.getBean(BusinessBean.class);
            Assertions.assertNotSame(proxyBean.getClass(), BusinessBean.class);
            Assertions.assertEquals("Hello, test.", proxyBean.hello("test"));
        }
    }

    PropertyResolver createPropertyResolver() {
        var ps = new Properties();
        var pr = new PropertyResolver(ps);
        return pr;
    }


}
