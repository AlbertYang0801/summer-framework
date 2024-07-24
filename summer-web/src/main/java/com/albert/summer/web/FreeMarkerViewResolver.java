package com.albert.summer.web;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * @author yjw
 * @date 2024/7/24 21:18
 */
public class FreeMarkerViewResolver implements ViewResolver {

    final String templatePath;
    final String templateEncoding;
    final ServletContext servletContext;

    Configuration config;

    public FreeMarkerViewResolver(String templatePath, String templateEncoding, ServletContext servletContext) {
        this.templatePath = templatePath;
        this.templateEncoding = templateEncoding;
        this.servletContext = servletContext;
    }

    @Override
    public void init() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setDefaultEncoding(this.templateEncoding);
        //cfg.setTemplateLoader(new ServletTemplateLoader(this.servletContext, this.templatePath));
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
        cfg.setLocalizedLookup(false);
        DefaultObjectWrapper ow = new DefaultObjectWrapper(Configuration.VERSION_2_3_32);
        ow.setExposeFields(true);
        cfg.setObjectWrapper(ow);
        this.config = cfg;
    }

    @Override
    public void render(String viewName, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {

    }
}
