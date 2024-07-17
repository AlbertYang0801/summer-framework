package com.albert.summer.context;

import com.albert.summer.property.PropertyResolver;
import com.albert.summer.scan.ScanApplication;
import com.albert.summer.scan.custom.annotation.CustomAnnotationBean;
import com.albert.summer.scan.nested.OuterBean;
import com.albert.summer.scan.primary.DogBean;
import com.albert.summer.scan.primary.PersonBean;
import com.albert.summer.scan.primary.TeacherBean;
import com.albert.summer.scan.sub1.Sub1Bean;
import com.albert.summer.scan.sub1.sub2.Sub2Bean;
import com.albert.summer.scan.sub1.sub2.sub3.Sub3Bean;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnnotationConfigApplicationContextTest {

    /**
     * 测试高层注解
     */
    @Test
    public void testCustomAnnotation() {
        var applicationContent = new AnnotationConfigApplicationContext(ScanApplication.class, createPropertyResolver());
        assertNotNull(applicationContent.getBean(CustomAnnotationBean.class));
        assertNotNull(applicationContent.getBean("customAnnotationBean"));
        assertNotNull(applicationContent.getBean("customAnnotation"));
    }

    /**
     * 测试import导入不在扫描路径的类
     */
    @Test
    public void testImport() {
        //变量解析器
        var applicationContent = new AnnotationConfigApplicationContext(ScanApplication.class, createPropertyResolver());
        assertNotNull(applicationContent.getBean(LocalDate.class));
        assertNotNull(applicationContent.getBean("startLocalDate"));
        assertNotNull(applicationContent.getBean("startLocalDateTime"));
        assertNotNull(applicationContent.getBean("startZonedDateTime"));
        System.out.println("done");
    }

    /**
     * 测试嵌套Bean
     */
    @Test
    public void testNestd() {
        var applicationContent = new AnnotationConfigApplicationContext(ScanApplication.class, createPropertyResolver());
        assertNotNull(applicationContent.getBean(OuterBean.class));
        assertNotNull(applicationContent.getBean(OuterBean.NestedBean.class));
    }

    /**
     * 测试@Primary
     */
    @Test
    public void tesPrimary() {
        var applicationContent = new AnnotationConfigApplicationContext(ScanApplication.class, createPropertyResolver());
        assertNotNull(applicationContent.getBean(PersonBean.class));

        var person = applicationContent.getBean(PersonBean.class);
        assertEquals(TeacherBean.class, person.getClass());

        DogBean dog = applicationContent.getBean(DogBean.class);
        assertEquals(dog.type,"Husky");
        //assertEquals(dog.type,"Teddy");
    }

    /**
     * 测试层级目录接口注入Bean
     */
    @Test
    public void testSub(){
        var applicationContent = new AnnotationConfigApplicationContext(ScanApplication.class, createPropertyResolver());
        assertNotNull(applicationContent.getBean(Sub1Bean.class));
        assertNotNull(applicationContent.getBean(Sub2Bean.class));
        assertNotNull(applicationContent.getBean(Sub3Bean.class));
    }


    PropertyResolver createPropertyResolver() {
        var ps = new Properties();
        ps.put("app.title", "Scan App");
        ps.put("app.version", "v1.0");
        ps.put("jdbc.url", "jdbc:hsqldb:file:testdb.tmp");
        ps.put("jdbc.username", "sa");
        ps.put("jdbc.password", "");
        ps.put("convert.boolean", "true");
        ps.put("convert.byte", "123");
        ps.put("convert.short", "12345");
        ps.put("convert.integer", "1234567");
        ps.put("convert.long", "123456789000");
        ps.put("convert.float", "12345.6789");
        ps.put("convert.double", "123456789.87654321");
        ps.put("convert.localdate", "2023-03-29");
        ps.put("convert.localtime", "20:45:01");
        ps.put("convert.localdatetime", "2023-03-29T20:45:01");
        ps.put("convert.zoneddatetime", "2023-03-29T20:45:01+08:00[Asia/Shanghai]");
        ps.put("convert.duration", "P2DT3H4M");
        ps.put("convert.zoneid", "Asia/Shanghai");
        var pr = new PropertyResolver(ps);
        return pr;
    }

}