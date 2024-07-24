package com.albert.summer.web;

import com.albert.summer.annotation.ResponseBody;
import com.albert.summer.exception.ServerErrorException;
import com.albert.summer.exception.ServerWebInputException;
import com.albert.summer.web.bean.Param;
import com.albert.summer.web.bean.Result;
import com.albert.summer.web.utils.JsonUtils;
import com.albert.summer.web.utils.PathUtils;
import com.albert.summer.web.utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Matcher;
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
     * 未处理状态
     */
    final static Result NOT_PROCESSED = new Result(false, null);

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
        //解析参数
        this.methodParams = new Param[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            //构建Param
            this.methodParams[i] = new Param(httpMethod, method, parameters[i], parameterAnnotations[i]);
        }
        log.debug("mapping {} to handler {}.{}", urlPattern, controller.getClass().getSimpleName(), method.getName());
    }

    /**
     * 调用方法
     * 解析入参，反射执行方法
     *
     * @param url
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    Result process(String url, HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        //TODO 为什么要正则匹配？
        //匹配URL
        Matcher matcher = urlPattern.matcher(url);
        if (matcher.matches()) {
            //获取参数真实值
            Object[] arguments = new Object[this.methodParams.length];
            for (int i = 0; i < arguments.length; i++) {
                Param param = methodParams[i];
                //@RequestBody、@RequestParam、@PathVariable
                arguments[i] = switch (param.paramType) {
                    case PATH_VARIABLE -> {
                        String group = matcher.group(param.name);
                        yield convertToType(param.classType, group);
                    }
                    case REQUEST_BODY -> {
                        //请求体
                        BufferedReader reader = request.getReader();
                        yield JsonUtils.readJson(reader, param.classType);
                    }
                    case REQUEST_PARAM -> {
                        String s = getOrDefault(request, param.name, param.defaultValue);
                        yield convertToType(param.classType, s);
                    }
                    case SERVICE_VARIABLE -> {
                        Class<?> classType = param.classType;
                        if (classType == HttpServletRequest.class) {
                            yield request;
                        } else if (classType == HttpServletResponse.class) {
                            yield response;
                        } else if (classType == HttpSession.class) {
                            yield request.getSession();
                        } else if (classType == ServletContext.class) {
                            yield request.getServletContext();
                        } else {
                            throw new ServerErrorException("Could not determine argument type: " + classType);
                        }
                    }
                };
            }
            Object result = null;
            //给某个类的某个方法，传入某些参数。反射执行方法
            try {
                result = this.handlerMethod.invoke(this.controller, arguments);
            } catch (InvocationTargetException e) {
                //Throwable t = e.getCause();
                //if (t instanceof Exception ex) {
                //    throw ex;
                //}
                //throw e;
            } catch (ReflectiveOperationException e) {
                throw new ServerErrorException(e);
            }
            return new Result(true, result);
        }
        //未匹配上，默认未处理
        return NOT_PROCESSED;
    }

    Object convertToType(Class<?> classType, String s) {
        if (classType == String.class) {
            return s;
        } else if (classType == boolean.class || classType == Boolean.class) {
            return Boolean.valueOf(s);
        } else if (classType == int.class || classType == Integer.class) {
            return Integer.valueOf(s);
        } else if (classType == long.class || classType == Long.class) {
            return Long.valueOf(s);
        } else if (classType == byte.class || classType == Byte.class) {
            return Byte.valueOf(s);
        } else if (classType == short.class || classType == Short.class) {
            return Short.valueOf(s);
        } else if (classType == float.class || classType == Float.class) {
            return Float.valueOf(s);
        } else if (classType == double.class || classType == Double.class) {
            return Double.valueOf(s);
        } else {
            throw new ServerErrorException("Could not determine argument type: " + classType);
        }
    }

    /**
     * 从request获取参数
     *
     * @param request
     * @param name
     * @param defaultValue
     * @return
     */
    String getOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String s = request.getParameter(name);
        if (s == null) {
            if (WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)) {
                throw new ServerWebInputException("Request parameter '" + name + "' not found.");
            }
            return defaultValue;
        }
        return s;
    }


}
