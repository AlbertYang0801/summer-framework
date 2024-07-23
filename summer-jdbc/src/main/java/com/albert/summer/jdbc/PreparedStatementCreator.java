package com.albert.summer.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author yangjunwei
 * @date 2024/7/23
 */
@FunctionalInterface
public interface PreparedStatementCreator {

    /**
     * 预编译SQL
     * @param connection
     * @return
     * @throws SQLException
     */
    PreparedStatement createPreparedStatement(Connection connection) throws SQLException;


}
