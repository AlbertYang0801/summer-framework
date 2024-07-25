package com.albert.summer.web;

import com.albert.summer.annotation.Autowired;
import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.annotation.Value;
import jakarta.servlet.ServletContext;

import java.util.Objects;

/**
 * web应用配置
 *
 * @author yangjunwei
 * @date 2024/7/24
 */
@Configuration
public class WebMvcConfiguration {

    private static ServletContext servletContext = null;

    static void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    /**
     * ServletContext本身是Servlet容器提供的，注入到IOC容器中
     *
     * @return
     */
    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set.");
    }

    @Bean(initMethod = "init")
    ViewResolver viewResolver(@Autowired ServletContext servletContext,
                              @Value("${summer.web.freemarker.template-path:/WEB-INF/templates}") String templatePath,
                              @Value("${summer.web.freemarker.template-encoding:UTF-8}") String templateEncoding) {
        return new FreeMarkerViewResolver(servletContext, templatePath, templateEncoding);
    }



}
