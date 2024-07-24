package com.albert.summer.web;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;
import jakarta.servlet.ServletContext;

import java.util.Objects;

/**
 * @author yangjunwei
 * @date 2024/7/24
 */
@Configuration
public class WebMvcConfiguration {

    private static ServletContext servletContext = null;

    static void setServletContext(ServletContext servletContext) {
        servletContext = servletContext;
    }

    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set.");
    }

}
