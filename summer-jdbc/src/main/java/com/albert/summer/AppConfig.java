package com.albert.summer;

import com.albert.summer.annotation.ComponentScan;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.annotation.Import;
import com.albert.summer.jdbc.JdbcConfiguration;

/**
 * 自动导入DataSource
 * @author yangjunwei
 * @date 2024/7/23
 */
@Import(JdbcConfiguration.class)
@ComponentScan
@Configuration
public class AppConfig {



}
