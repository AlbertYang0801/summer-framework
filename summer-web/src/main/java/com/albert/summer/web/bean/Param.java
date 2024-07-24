package com.albert.summer.web.bean;

import com.albert.summer.annotation.PathVariable;
import com.albert.summer.annotation.RequestBody;
import com.albert.summer.annotation.RequestParam;
import com.albert.summer.exception.ServerErrorException;
import com.albert.summer.utils.ClassUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjunwei
 * @date 2024/7/24
 */
public class Param {

    public String name;

    public ParamType paramType;

    public Class<?> classType;

    public String defaultValue;

    private static List<Class<?>> SERVICE_VARIABLE_CLASSES = new ArrayList<>();

    static {
        SERVICE_VARIABLE_CLASSES.add(HttpServletRequest.class);
        SERVICE_VARIABLE_CLASSES.add(HttpServletResponse.class);
        SERVICE_VARIABLE_CLASSES.add(HttpSession.class);
        SERVICE_VARIABLE_CLASSES.add(ServletContext.class);
    }

    /**
     * 解析Controller层的接口入参
     * 1.参数注解
     * 2.Servlet提供的入参
     *
     * @param httpMethod  方法名
     * @param method      方法
     * @param parameter   参数
     * @param annotations 注解(参数注解)
     */
    public Param(String httpMethod, Method method, Parameter parameter, Annotation[] annotations) throws ServletException {
        //约定的三种参数注解
        PathVariable pathVariable = ClassUtils.getAnnotation(annotations, PathVariable.class);
        RequestParam requestParam = ClassUtils.getAnnotation(annotations, RequestParam.class);
        RequestBody requestBody = ClassUtils.getAnnotation(annotations, RequestBody.class);
        int total = (pathVariable == null ? 0 : 1) + (requestParam == null ? 0 : 1) + (requestBody == null ? 0 : 1);
        //只允许一种参数注解
        if (total > 1) {
            throw new ServletException("Annotation @PathVariable, @RequestParam and @RequestBody cannot be combined at method: " + method);
        }
        //参数类型
        this.classType = parameter.getType();
        if (pathVariable != null) {
            this.name = pathVariable.value();
            this.paramType = ParamType.PATH_VARIABLE;
        } else if (requestParam != null) {
            this.name = requestParam.value();
            this.paramType = ParamType.REQUEST_PARAM;
            this.defaultValue = requestParam.defaultValue();
        } else if (requestBody != null) {
            //从请求体获取数据
            this.paramType = ParamType.REQUEST_BODY;
        } else {
            //非Servlet自身提供的入参，报错
            if (!SERVICE_VARIABLE_CLASSES.contains(this.classType)) {
                throw new ServerErrorException("(Missing annotation?) Unsupported argument type: " + classType + " at method: " + method);
            }
            this.paramType = ParamType.SERVICE_VARIABLE;
        }
    }


    @Override
    public String toString() {
        return "Param [name=" + name + ", paramType=" + paramType + ", classType=" + classType + ", defaultValue=" + defaultValue + "]";
    }


}
