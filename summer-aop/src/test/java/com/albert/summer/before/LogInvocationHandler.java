package com.albert.summer.before;

import com.albert.summer.annotation.Component;
import com.albert.summer.aop.BeforeInvocationHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * @author yjw
 * @date 2024/7/22 22:02
 */
@Slf4j
@Component
public class LogInvocationHandler extends BeforeInvocationHandlerAdapter {

    @Override
    public void before(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("[Before] {}", method.getName());
    }


}
