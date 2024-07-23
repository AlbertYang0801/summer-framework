package com.albert.summer.annotation;

import java.lang.annotation.*;

/**
 * @author yangjunwei
 * @date 2024/7/23
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
//父类继承该注解
@Inherited
public @interface Transactional {

    String value() default "platformTransactionManager";

    /**
     * 按照指定异常回滚
     * @return
     */
    Class<? extends Throwable>[] rollbackFor() default {};

}
