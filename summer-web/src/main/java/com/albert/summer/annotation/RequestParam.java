package com.albert.summer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";

    String value();

    String defaultValue() default DEFAULT_PARAM_VALUE;


}
