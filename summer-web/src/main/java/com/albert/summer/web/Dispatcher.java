package com.albert.summer.web;

import com.albert.summer.annotation.ResponseBody;
import com.albert.summer.web.bean.Param;
import com.albert.summer.web.utils.PathUtils;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Pattern;

/**
 * Controller层URL处理器，每一个Dispatcher对应一个接口
 *
 * @author yangjunwei
 * @date 2024/7/24
 */
@Slf4j
public class Dispatcher {

    /**
     * 是否返回Rest
     */
    boolean isRest;
    /**
     * 是否有@ResponseBody
     */
    boolean isResponseBody;
    /**
     * 是否void
     */
    boolean isVoid;
    /**
     * URL正则匹配
     */
    Pattern urlPattern;
    /**
     * Bean实例
     */
    Object controller;
    /**
     * 处理方法
     */
    Method handlerMethod;
    /**
     * 方法的请求参数
     */
    Param[] methodParams;

    /**
     * @param httpMethod
     * @param isRest
     * @param controller
     * @param method     具体方法
     * @param urlPattern
     */
    public Dispatcher(String httpMethod, boolean isRest, Object controller, Method method, String urlPattern) throws ServletException {
        this.isRest = isRest;
        this.isResponseBody = method.getAnnotation(ResponseBody.class) != null;
        this.isVoid = method.getReturnType() == void.class;
        //解析URL正则
        this.urlPattern = PathUtils.compile(urlPattern);
        this.controller = controller;
        this.handlerMethod = method;
        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        this.methodParams = new Param[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            //构建Param
            this.methodParams[i] = new Param(httpMethod, method, parameters[i], parameterAnnotations[i]);
        }
        log.debug("mapping {} to handler {}.{}", urlPattern, controller.getClass().getSimpleName(), method.getName());
    }


}
