package com.albert.summer.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

import static cn.hutool.extra.spring.SpringUtil.getApplicationContext;

/**
 * @author yangjunwei
 * @date 2024/7/19
 */
public class ApplicationContextUtils {

    private static ApplicationContext applicationContext;

    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not set");
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextUtils.applicationContext = applicationContext;
    }


}
