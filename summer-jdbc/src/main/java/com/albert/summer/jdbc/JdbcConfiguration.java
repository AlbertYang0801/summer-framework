package com.albert.summer.jdbc;

import com.albert.summer.annotation.Autowired;
import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.annotation.Value;
import com.albert.summer.tx.DataSourceTransactionManager;
import com.albert.summer.tx.PlatformTransactionManager;
import com.albert.summer.tx.TransactionalBeanPostProcessor;
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
        HikariConfig config = new HikariConfig();
        //自动提交，设置为false，需要手动提交
        config.setAutoCommit(true);
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

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource){
        return new JdbcTemplate(dataSource);
    }

    @Bean
    TransactionalBeanPostProcessor transactionalBeanPostProcessor(){
        return new TransactionalBeanPostProcessor();
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource){
        return new DataSourceTransactionManager(dataSource);
    }



}
