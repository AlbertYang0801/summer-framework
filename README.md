# summer-framework

## 介绍

summer-framework 轻量级 Spring框架

## 软件架构

软件架构说明

- summer-context
- summer-aop
- summer-jdbc
- summer-web
- summer-boot

### summer-context

1. 扫描Bean
2. 属性解析器
3. BeanDefinition定义
4. 解析@Configuration工厂类
4. 解析BeanPostProcessor
5. 创建Bean
6. 属性注入

### summer-aop

1. 实现AOP功能
2. 使用ByteBuddy进行字节码增强
3. 实现Around和Before逻辑

### summer-jdbc

1. 实现JdbcTemplate
2. 声明式事务管理

### summer-web

1. `DispatcherServlet`作为核心处理组件，接收所有URL请求，然后按MVC规则转发；
2. 基于`@Controller`注解的URL控制器，由应用程序提供，Spring负责解析规则；
3. 提供`ViewResolver`，将应用程序的Controller处理后的结果进行渲染，给浏览器返回页面；
4. 基于`@RestController`注解的REST处理机制，由应用程序提供，Spring负责将输入输出变为JSON格式；
5. 多种拦截器和异常处理器等。


### summer-boot

1. 内置tomcat
2. 自动注入配置类