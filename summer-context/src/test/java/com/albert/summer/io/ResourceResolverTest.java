package com.albert.summer.io;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
public class ResourceResolverTest {

    /**
     * 测试扫描指定包的所有类
     */
    @Test
    public void testScanClass() {
        var pkg = "com.albert.summer.scan";
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        Collections.sort(classes);
        for (String aClass : classes) {
            System.out.println(aClass);
        }
    }

    /**
     * 测试扫描某个jar包下面所有的类
     */
    @Test
    public void testScanJar(){
        var pkg = PostConstruct.class.getPackageName();
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        Collections.sort(classes);
        for (String aClass : classes) {
            System.out.println(aClass);
        }
    }


}
