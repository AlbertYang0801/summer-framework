package com.albert.summer.jdbc;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.annotation.Value;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * DataSource工厂
 *
 * @author yangjunwei
 * @date 2024/7/23
 */
@Configuration
public class JdbcConfiguration {

    /**
     * 自动注入HikariDataSource
     *
     * @param url
     * @param username
     * @param password
     * @param driver
     * @param maximumPoolSize
     * @param minimumPoolSize
     * @param connTimeout
     * @return
     */
    @Bean
    DataSource dataSource(@Value("${summer.datasource.url}") String url,
                          @Value("${summer.datasource.username}") String username,
                          @Value("${summer.datasource.password}") String password,
                          @Value("${summer.datasource.driver-class-name:}") String driver,
                          @Value("${summer.datasource.maximum-pool-size:20}") int maximumPoolSize,
                          @Value("${summer.datasource.minimum-pool-size:1}") int minimumPoolSize,
                          @Value("${summer.datasource.connection-timeout:30000}") int connTimeout) {
        var config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }


}
