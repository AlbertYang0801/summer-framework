package com.albert.summer.scan.proxy;

import com.albert.summer.annotation.Autowired;
import com.albert.summer.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {

    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}
