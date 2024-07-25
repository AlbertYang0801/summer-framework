package com.albert.summer.web;

import jakarta.servlet.Filter;

import java.util.List;

/**
 * 过滤器
 * @author yangjunwei
 * @date 2024/7/25
 */
public abstract class FileterRegistrationBean {

    public abstract List<String> getUrlPatterns();

    public abstract Filter getFilter();

    /**
     * Get name by class name. Example:
     *
     * ApiFilterRegistrationBean -> apiFilter
     *
     * ApiFilterRegistration -> apiFilter
     *
     * ApiFilterReg -> apiFilterReg
     */
    public String getName() {
        String name = getClass().getSimpleName();
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        if (name.endsWith("FilterRegistrationBean") && name.length() > "FilterRegistrationBean".length()) {
            return name.substring(0, name.length() - "FilterRegistrationBean".length());
        }
        if (name.endsWith("FilterRegistration") && name.length() > "FilterRegistration".length()) {
            return name.substring(0, name.length() - "FilterRegistration".length());
        }
        return name;
    };



}
