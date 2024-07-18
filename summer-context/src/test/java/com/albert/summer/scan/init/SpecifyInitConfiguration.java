package com.albert.summer.scan.init;


import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.annotation.Value;

@Configuration
public class SpecifyInitConfiguration {

    /**
     * 工厂方法注入Bean
     * 入参属性注入
     * @param appTitle
     * @param appVersion
     * @return
     */
    @Bean(initMethod = "init")
    SpecifyInitBean createSpecifyInitBean(@Value("${app.title}") String appTitle, @Value("${app.version}") String appVersion) {
        return new SpecifyInitBean(appTitle, appVersion);
    }

}
