package com.albert.summer.scan.proxy;


import com.albert.summer.annotation.Component;
import com.albert.summer.annotation.Value;

@Component
public class OriginBean {

    @Value("${app.title}")
    public String name;

    public String version;

    @Value("${app.version}")
    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return this.version;
    }
}
