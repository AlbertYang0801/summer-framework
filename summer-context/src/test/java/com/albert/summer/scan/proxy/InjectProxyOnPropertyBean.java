package com.albert.summer.scan.proxy;

import com.albert.summer.annotation.Autowired;
import com.albert.summer.annotation.Component;

@Component
public class InjectProxyOnPropertyBean {

    @Autowired
    public OriginBean injected;
}
