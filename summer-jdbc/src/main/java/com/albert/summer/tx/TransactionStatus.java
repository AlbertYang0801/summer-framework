package com.albert.summer.tx;

import java.sql.Connection;

/**
 * 表示当前事务状态
 * 可以存储事务的传播行为
 * @author yangjunwei
 * @date 2024/7/23
 */
public class TransactionStatus {

    Connection connection;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }

}
