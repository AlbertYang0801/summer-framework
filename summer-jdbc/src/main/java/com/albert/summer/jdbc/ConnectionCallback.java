package com.albert.summer.jdbc;

import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection回调
 *
 * @author yangjunwei
 * @date 2024/7/23
 */
@FunctionalInterface
public interface ConnectionCallback<T> {

    @Nullable
    T doInConnection(Connection connection) throws SQLException;


}
