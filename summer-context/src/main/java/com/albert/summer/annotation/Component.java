package com.albert.summer.annotation;

import java.lang.annotation.*;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {

    /**
     * Bean name. Default to simple class name with first-letter-lowercase.
     */
    String value() default "";


}
