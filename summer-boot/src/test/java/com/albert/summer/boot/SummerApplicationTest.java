package com.albert.summer.boot;

import org.apache.catalina.LifecycleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SummerApplicationTest {

    @Test
    public void testRun() {
        try {
            SummerApplication.run("src/main/webapp","target/classes",HelloConfiguration.class,"");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}