package com.albert.summer.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.annotation.Nullable;

@FunctionalInterface
public interface RowMapper<T> {

    /**
     * 处理结果集
     * @param rs
     * @param rowNum
     * @return
     * @throws SQLException
     */
    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
