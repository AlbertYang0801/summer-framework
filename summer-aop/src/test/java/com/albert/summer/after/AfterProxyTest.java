package com.albert.summer.after;

import com.albert.summer.context.AnnotationConfigApplicationContext;
import com.albert.summer.property.PropertyResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

/**
 * @author yjw
 * @date 2024/7/22 21:56
 */
public class AfterProxyTest {

    @Test
    public void testAfterProxy() throws IOException {
        try (var ctx = new AnnotationConfigApplicationContext(AfterApplication.class, createPropertyResolver())) {
            GreetingBean proxy = ctx.getBean(GreetingBean.class);
            // should change return value:
            Assertions.assertEquals("Hello, Bob!", proxy.hello("Bob"));
            Assertions.assertEquals("Morning, Alice!", proxy.morning("Alice"));
        }

    }

    PropertyResolver createPropertyResolver() {
        var ps = new Properties();
        var pr = new PropertyResolver(ps);
        return pr;
    }


}
