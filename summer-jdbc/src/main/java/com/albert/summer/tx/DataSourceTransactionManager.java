package com.albert.summer.tx;

import com.albert.summer.exception.TransactionException;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 真正执行开启、提交、回滚事务的地方
 * 依赖动态代理实现，在原始Bean的基础上，扩展事务的逻辑
 *
 * @author yangjunwei
 * @date 2024/7/23
 */
@Slf4j
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
        //TODO 解析proxyBean的@Transactional

        TransactionStatus transactionStatus = TRANSACTION_STATUS.get();
        //当前事务
        if (transactionStatus == null) {
            try (Connection connection = dataSource.getConnection()) {
                boolean autoCommit = connection.getAutoCommit();
                if (autoCommit) {
                    //取消自动提交，开启本地事务
                    connection.setAutoCommit(false);
                }
                try {
                    //保存当前事务
                    TRANSACTION_STATUS.set(new TransactionStatus(connection));
                    //继续执行业务方法
                    Object invoke = method.invoke(proxy, args);

                    //提交事务
                    connection.commit();

                    //方法返回
                    return invoke;
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO 集合事务注解的rollBack
                    //回滚事务
                    TransactionException transactionException = new TransactionException(e);
                    try {
                        connection.rollback();
                    } catch (SQLException sqlException) {
                        //添加SQLException到TranscationException
                        transactionException.addSuppressed(sqlException);
                    }
                    throw transactionException;

                } finally {
                    TRANSACTION_STATUS.remove();
                    if (autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        } else {
            //TODO 默认事务传播行为 REQUIRED
            //当前已有事务，加入当前事务执行
            return method.invoke(proxy, args);
        }
    }


}
