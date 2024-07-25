package com.albert.summer.web;

import com.albert.summer.context.AnnotationConfigApplicationContext;
import com.albert.summer.context.ApplicationContext;
import com.albert.summer.property.PropertyResolver;
import com.albert.summer.web.utils.WebUtils;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * 初始化Servlet容器
 *
 * @author yangjunwei
 * @date 2024/7/25
 */
@Slf4j
public class ContextLoaderInitializer implements ServletContainerInitializer {

    final Class<?> configClass;
    final PropertyResolver propertyResolver;

    public ContextLoaderInitializer(Class<?> configClass, PropertyResolver propertyResolver) {
        this.configClass = configClass;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext) throws ServletException {
        log.info("Servlet container start. ServletContext = {}", servletContext);

        String encoding = propertyResolver.getProperty("${summer.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);

        //设置Servlet容器
        WebMvcConfiguration.setServletContext(servletContext);
        //注册IOC容器
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(this.configClass, this.propertyResolver);
        log.info("Application context loaded : {}", applicationContext);
        WebUtils.registerFilters(servletContext);
        //注册MVC主要类 - DispatcherServlet
        WebUtils.registerDispatcherServlet(servletContext, this.propertyResolver);
    }


}
