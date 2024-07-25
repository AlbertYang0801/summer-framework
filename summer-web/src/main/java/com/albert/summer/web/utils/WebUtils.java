package com.albert.summer.web.utils;

import com.albert.summer.context.ApplicationContextUtils;
import com.albert.summer.property.PropertyResolver;
import com.albert.summer.utils.ClassPathUtils;
import com.albert.summer.utils.YamlUtils;
import com.albert.summer.web.DispatcherServlet;
import com.albert.summer.web.FileterRegistrationBean;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

@Slf4j
public class WebUtils {

    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";

    static final String CONFIG_APP_YAML = "/application.yml";
    static final String CONFIG_APP_PROP = "/application.properties";

    /**
     * x向ServletContext注入DispatcherServlet
     * @param servletContext
     * @param properyResolver
     */
    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver properyResolver) {
        var dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), properyResolver);
        log.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        //将DisPatcherServlet加入到Servlet容器中
        var dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        //根目录映射
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }

    /**
     * ServletContext添加过滤器
     * @param servletContext
     */
    public static void registerFilters(ServletContext servletContext) {
        var applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        for (var filterRegBean : applicationContext.getBeans(FileterRegistrationBean.class)) {
            List<String> urlPatterns = filterRegBean.getUrlPatterns();
            if (urlPatterns == null || urlPatterns.isEmpty()) {
                throw new IllegalArgumentException("No url patterns for {}" + filterRegBean.getClass().getName());
            }
            var filter = Objects.requireNonNull(filterRegBean.getFilter(), "FilterRegistrationBean.getFilter() must not return null.");
            log.info("register filter '{}' {} for URLs: {}", filterRegBean.getName(), filter.getClass().getName(), String.join(", ", urlPatterns));
            var filterReg = servletContext.addFilter(filterRegBean.getName(), filter);
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns.toArray(String[]::new));
        }
    }

    /**
     * Try load property resolver from /application.yml or /application.properties.
     */
    public static PropertyResolver createPropertyResolver() {
        final Properties props = new Properties();
        // try load application.yml:
        try {
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            log.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if (value instanceof String strValue) {
                    props.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                // try load application.properties:
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, (input) -> {
                    log.info("load config: {}", CONFIG_APP_PROP);
                    props.load(input);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }


}
