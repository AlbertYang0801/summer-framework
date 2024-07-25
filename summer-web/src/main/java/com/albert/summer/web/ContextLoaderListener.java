package com.albert.summer.web;

import com.albert.summer.context.AnnotationConfigApplicationContext;
import com.albert.summer.context.ApplicationContext;
import com.albert.summer.exception.NestedRuntimeException;
import com.albert.summer.property.PropertyResolver;
import com.albert.summer.web.utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;

/**
 * 监听Servlet的创建和销毁
 *
 * @author yangjunwei
 * @date 2024/7/24
 */
@Slf4j
public class ContextLoaderListener implements ServletContextListener {

    /**
     * 监听Servlet容器初始化
     * 1.初始化IOC容器
     * 2.创建DispatcherServlet
     *
     * @param sce
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("init {}", getClass().getName());

        //Servlet容器
        ServletContext servletContext = sce.getServletContext();
        WebMvcConfiguration.setServletContext(servletContext);

        //application.yml中的配置内容
        //配置解析器
        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${summer.web.character-encoding:UTF-8}");

        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setRequestCharacterEncoding(encoding);

        //servlet的配置，web.xml中配置
        ApplicationContext applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);

        WebUtils.registerFilters(servletContext);
        WebUtils.registerDispatcherServlet(servletContext,propertyResolver);
    }

    /**
     * 根据配置的Configuration类加载IOC容器
     *
     * @param configClassName
     * @param propertyResolver
     * @return
     */
    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        log.info("init ApplicationContext by configuration:{}", configClassName);
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
        try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': " + configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }


}
