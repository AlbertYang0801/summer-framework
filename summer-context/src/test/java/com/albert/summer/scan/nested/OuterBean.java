package com.albert.summer.scan.nested;


import com.albert.summer.annotation.Component;

@Component
public class OuterBean {

    @Component
    public static class NestedBean {

    }
}
