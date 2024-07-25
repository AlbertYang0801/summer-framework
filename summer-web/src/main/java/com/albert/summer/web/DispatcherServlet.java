package com.albert.summer.web;

import cn.hutool.json.JSONUtil;
import com.albert.summer.annotation.Controller;
import com.albert.summer.annotation.GetMapping;
import com.albert.summer.annotation.PostMapping;
import com.albert.summer.annotation.RestController;
import com.albert.summer.context.ApplicationContext;
import com.albert.summer.context.BeanDefinition;
import com.albert.summer.context.ConfigurableApplicationContext;
import com.albert.summer.exception.ErrorResponseException;
import com.albert.summer.exception.NestedRuntimeException;
import com.albert.summer.property.PropertyResolver;
import com.albert.summer.web.bean.ModelAndView;
import com.albert.summer.web.bean.Result;
import com.albert.summer.web.utils.JsonUtils;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * DispatcherServlet
 * MVC核心处理器，接收所有url请求，按照MVC规则转发
 *
 * @author yangjunwei
 * @date 2024/7/24
 */
@Slf4j
public class DispatcherServlet extends HttpServlet {

    ApplicationContext applicationContext;
    ViewResolver viewResolver;

    List<Dispatcher> getDispatchers = new ArrayList<>();
    List<Dispatcher> postDispatchers = new ArrayList<>();

    /**
     * 静态资源路径
     */
    String resourcePath;
    /**
     * 静态图标
     */
    String faviconPath;

    public DispatcherServlet(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public DispatcherServlet(ApplicationContext requiredApplicationContext, PropertyResolver properyResolver) {
        this.applicationContext = requiredApplicationContext;
        //TODO
        this.viewResolver = applicationContext.getBean(ViewResolver.class);
        this.resourcePath = properyResolver.getProperty("${summer.web.static-path:/static/}");
        this.faviconPath = properyResolver.getProperty("${summer.web.favicon-path:/favicon.ico}");
        if (!this.resourcePath.endsWith("/")) {
            this.resourcePath = this.resourcePath + "/";
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("init {}", getClass().getName());
        //scan @Controller 和 @RestController，注册方法（每个方法对应一个url）
        //查询所有Bean
        for (BeanDefinition def : ((ConfigurableApplicationContext) this.applicationContext).findBeanDefinitions(Object.class)) {
            Class<?> beanClass = def.getBeanClass();
            Object requiredInstance = def.getRequiredInstance();

            Controller controller = beanClass.getAnnotation(Controller.class);
            RestController restController = beanClass.getAnnotation(RestController.class);
            if (controller != null && restController != null) {
                throw new ServletException("Found @Controller and @RestController on class: " + beanClass.getName());
            }

            if (controller != null) {
                addController(false, def.getName(), requiredInstance);
            }

            if (restController != null) {
                addController(true, def.getName(), requiredInstance);
            }
        }
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    void addController(boolean isRest, String name, Object instance) throws ServletException {
        log.info("add {} controller '{}': {}", isRest ? "REST" : "MVC", name, instance.getClass().getName());
        addMethods(isRest, name, instance, instance.getClass());
    }

    void addMethods(boolean isRest, String name, Object instance, Class<?> type) throws ServletException {
        //获取当前类的所有方法
        for (Method m : type.getDeclaredMethods()) {
            //解析GetMapping
            GetMapping get = m.getAnnotation(GetMapping.class);
            if (get != null) {
                checkMethod(m);
                this.getDispatchers.add(new Dispatcher("GET", isRest, instance, m, get.value()));
            }
            //解析PostMapping
            PostMapping post = m.getAnnotation(PostMapping.class);
            if (post != null) {
                checkMethod(m);
                //对应的dispatcher
                this.postDispatchers.add(new Dispatcher("POST", isRest, instance, m, post.value()));
            }
        }
        //扫描父类方法
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            addMethods(isRest, name, instance, superClass);
        }
    }

    void checkMethod(Method m) throws ServletException {
        //获取方法的修饰符信息
        //public、private
        int mod = m.getModifiers();
        //静态方法不能加URL
        if (Modifier.isStatic(mod)) {
            throw new ServletException("Cannot do URL mapping to static method: " + m);
        }
        //修改方法访问权限为可见
        m.setAccessible(true);
    }

    /**
     * Servlet默认方法，get请求都会进到该方法
     * 从所有Get接口中，匹配url
     * 匹配上url之后执行方法，根据注解解析返回结果
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();

        //区分静态资源请求，还是业务URL请求
        if (url.equals(this.faviconPath) || url.startsWith(this.resourcePath)) {
            doResource(url, req, resp);
        } else {
            doService(req, resp, this.getDispatchers);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp, this.postDispatchers);
    }

    /**
     * @param req
     * @param resp
     * @param dispatchers
     * @throws ServletException
     * @throws IOException
     */
    void doService(HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws ServletException, IOException {
        String url = req.getRequestURI();
        try {
            doService(url, req, resp, dispatchers);
        } catch (ErrorResponseException e) {
            log.warn("process request failed with status " + e.statusCode + ":" + url, e);
            if (!resp.isCommitted()) {
                resp.resetBuffer();
                resp.sendError(e.statusCode);
            }
        } catch (RuntimeException | ServletException | IOException e) {
            log.warn("process request failed: " + url, e);
            throw e;
        } catch (Exception e) {
            log.warn("process request failed: " + url, e);
            throw new NestedRuntimeException(e);
        }
    }

    /**
     * 处理Controller方法返回结果
     *
     * @param url
     * @param req
     * @param resp
     * @param dispatchers
     * @throws ServletException
     * @throws IOException
     */
    void doService(String url, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws ServletException, IOException {
        for (Dispatcher dispatcher : dispatchers) {
            //匹配url，执行方法
            Result process = dispatcher.process(url, req, resp);
            //url匹配成功，方法执行完成
            if (process.processed()) {
                //执行结果
                //Controller层方法返回结果
                Object processResult = process.returnObject();
                //restController需要返回JSON
                if (dispatcher.isRest) {
                    if (!resp.isCommitted()) {
                        resp.setContentType("application/json");
                    }
                    //@ResponseBody只能处理String、byte[]?
                    if (dispatcher.isResponseBody) {
                        //instanceof 后强转
                        if (processResult instanceof String s) {
                            PrintWriter writer = resp.getWriter();
                            writer.write(s);
                            writer.flush();
                        } else if (processResult instanceof byte[] data) {
                            ServletOutputStream outputStream = resp.getOutputStream();
                            outputStream.write(data);
                            outputStream.flush();
                        } else {
                            //序列化对象为json然后返回
                            PrintWriter writer = resp.getWriter();
                            JsonUtils.writeJson(writer, processResult);
                            writer.flush();
                            //throw new ServletException("Unable to process REST result when handle url: " + url);
                        }
                    } else if (!dispatcher.isVoid) {
                        //TODO 加了@ResponseBody的接口，不能序列化对象？
                        //默认转换为JSON
                        PrintWriter writer = resp.getWriter();
                        JsonUtils.writeJson(writer, processResult);
                        writer.flush();
                    }
                } else {
                    //process MVC
                    if (!resp.isCommitted()) {
                        resp.setContentType("text/html");
                    }
                    if (processResult instanceof String s) {
                        if (dispatcher.isResponseBody) {
                            PrintWriter writer = resp.getWriter();
                            writer.write(s);
                            writer.flush();
                        } else if (s.startsWith("redirect:")) {
                            // send redirect:
                            resp.sendRedirect(s.substring(9));
                        } else {
                            // error:
                            throw new ServletException("Unable to process String result when handle url: " + url);
                        }
                    } else if (processResult instanceof byte[] data) {
                        if (dispatcher.isResponseBody) {
                            // send as response body:
                            ServletOutputStream output = resp.getOutputStream();
                            output.write(data);
                            output.flush();
                        } else {
                            // error:
                            throw new ServletException("Unable to process byte[] result when handle url: " + url);
                        }
                    } else if (processResult instanceof ModelAndView mv) {
                        String view = mv.getViewName();
                        if (view.startsWith("redirect:")) {
                            // send redirect:
                            resp.sendRedirect(view.substring(9));
                        } else {
                            this.viewResolver.render(view, mv.getModel(), req, resp);
                        }
                    } else if (!dispatcher.isVoid && processResult != null) {
                        // error:
                        throw new ServletException("Unable to process " + processResult.getClass().getName() + " result when handle url: " + url);
                    }
                }

            }
            return;
        }
        resp.sendError(404, "NOT FOUND");
    }

    void doResource(String url, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletContext ctx = req.getServletContext();
        try (InputStream input = ctx.getResourceAsStream(url)) {
            if (input == null) {
                resp.sendError(404, "Not Found");
            } else {
                // guess content type:
                String file = url;
                int n = url.lastIndexOf('/');
                if (n >= 0) {
                    file = url.substring(n + 1);
                }
                String mime = ctx.getMimeType(file);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);
                ServletOutputStream output = resp.getOutputStream();
                input.transferTo(output);
                output.flush();
            }
        }
    }


}
