package com.albert.summer.annotation;/**
 * @author yjw
 * @date 2024/7/22 21:08
 */

import java.lang.annotation.*;

/**
 * @author Albert.Yang
 * @date 2024/7/22
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Before {
}
