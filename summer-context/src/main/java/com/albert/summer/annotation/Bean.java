package com.albert.summer.annotation;

import java.lang.annotation.*;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

    /**
     * Bean name. default to method name.
     */
    String value() default "";

    String initMethod() default "";

    String destroyMethod() default "";


}
