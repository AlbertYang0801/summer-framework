package com.albert.summer.around;


import com.albert.summer.annotation.Autowired;
import com.albert.summer.annotation.Component;
import com.albert.summer.annotation.Order;

@Order(0)
@Component
public class OtherBean {

    public OriginBean origin;

    public OtherBean(@Autowired OriginBean origin) {
        this.origin = origin;
    }
}
