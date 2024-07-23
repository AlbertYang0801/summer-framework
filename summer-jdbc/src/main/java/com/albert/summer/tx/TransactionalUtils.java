package com.albert.summer.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

/**
 * @author yjw
 * @date 2024/7/23 20:28
 */
public class TransactionalUtils {

    /**
     * 获取当前事务
     * 其它方法可以加入当前事务
     */
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus transactionStatus = DataSourceTransactionManager.TRANSACTION_STATUS.get();
        if (transactionStatus == null) {
            return null;
        }
        //获取当前事务的Connection
        return transactionStatus.connection;
    }


}
