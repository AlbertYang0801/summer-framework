package com.albert.summer.bean;

import cn.hutool.db.ds.simple.SimpleDataSource;
import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
@Configuration
public class AppConfig {

    @Bean(initMethod="init", destroyMethod="close")
    DataSource createDataSource() {
        return new SimpleDataSource();
    }

}
