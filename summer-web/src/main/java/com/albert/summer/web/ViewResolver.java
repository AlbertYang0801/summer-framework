package com.albert.summer.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * @author yjw
 * @date 2024/7/24 21:16
 */
public interface ViewResolver {

    /**
     * 初始化
     */
    void init();

    /**
     * 渲染
     * @param viewName
     * @param model
     * @param request
     * @param response
     */
    void render(String viewName, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response);

}
