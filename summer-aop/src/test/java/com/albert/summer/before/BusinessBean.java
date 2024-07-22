package com.albert.summer.before;


import com.albert.summer.annotation.Around;
import com.albert.summer.annotation.Component;

@Component
@Around("logInvocationHandler")
public class BusinessBean {

    public String hello(String name) {
        return "Hello, " + name + ".";
    }

    public String morning(String name) {
        return "Morning, " + name + ".";
    }
}
