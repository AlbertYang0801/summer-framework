package com.albert.summer.boot;


import com.albert.summer.annotation.ComponentScan;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.annotation.Import;
import com.albert.summer.jdbc.JdbcConfiguration;
import com.albert.summer.web.WebMvcConfiguration;

@ComponentScan
@Configuration
@Import({JdbcConfiguration.class, WebMvcConfiguration.class})
public class HelloConfiguration {

}
