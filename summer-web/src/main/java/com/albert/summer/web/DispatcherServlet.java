package com.albert.summer.web;

import com.albert.summer.context.ApplicationContext;
import com.albert.summer.property.PropertyResolver;
import com.albert.summer.web.bean.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
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


    public DispatcherServlet(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public DispatcherServlet(ApplicationContext requiredApplicationContext, PropertyResolver properyResolver) {
        this.applicationContext = requiredApplicationContext;
        //TODO
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        //依次匹配配置的Controller层接口
        for (Dispatcher dispatcher : getDispatchers) {
            //匹配URL
            Result process = dispatcher.process(uri, req, resp);
            if (process.processed()) {
                //处理结果

            }
            return;
        }
        //请求未匹配到任何Dispatcher
        resp.sendError(404, "NOT FOUND");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }


}
