package com.albert.summer.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.annotation.Nullable;

/**
 * 预编译回调
 * @param <T>
 * @author admin
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {

    @Nullable
    T doInPreparedStatement(PreparedStatement ps) throws SQLException;

}
