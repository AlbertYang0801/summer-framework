package com.albert.summer.annotation;

import java.lang.annotation.*;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

    /**
     * Is required.
     */
    boolean value() default true;

    /**
     * Bean name if set.
     */
    String name() default "";


}