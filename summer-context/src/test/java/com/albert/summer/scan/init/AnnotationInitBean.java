package com.albert.summer.scan.init;


import com.albert.summer.annotation.Component;
import com.albert.summer.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class AnnotationInitBean {

    @Value("${app.title}")
    String appTitle;

    @Value("${app.version}")
    String appVersion;

    public String appName;

    @PostConstruct
    void init() {
        this.appName = this.appTitle + " / " + this.appVersion;
    }

    public String getAppName() {
        return appName;
    }

    @PreDestroy
    public void destroy() {
        System.out.println("AnnotationInitBean destroy!");
    }

}
