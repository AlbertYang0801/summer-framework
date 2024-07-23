package com.albert.summer.tx;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 真正执行开启、提交、回滚事务的地方
 * 依赖动态代理实现，在原始Bean的基础上，扩展事务的逻辑
 * @author yangjunwei
 * @date 2024/7/23
 */
public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {

    /**
     * 使用ThreadLocal保存事务状态（包括事务传播行为）
     */
    static final ThreadLocal<TransactionStatus> TRANSACTION_STATUS = new ThreadLocal<>();

    final DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 基于动态代理实现事务方法
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }


}
