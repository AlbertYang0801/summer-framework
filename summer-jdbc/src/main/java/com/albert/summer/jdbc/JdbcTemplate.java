package com.albert.summer.jdbc;

import com.albert.summer.exception.DataAccessException;
import jakarta.annotation.Nullable;

import javax.sql.DataSource;
import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjunwei
 * @date 2024/7/23
 */
public class JdbcTemplate {

    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 使用dataSource执行数据库操作
     *
     * @param action
     * @param <T>
     * @return
     */
    public <T> T execute(ConnectionCallback<T> action) {
        try (Connection connection = dataSource.getConnection()) {
            //提供Connection，供上层代码使用
            T t = action.doInConnection(connection);
            return t;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * update
     *
     * @param sql
     * @param params
     * @return
     */
    public int update(String sql, Object... params) {
        //预编译SQL，然后执行update
        return execute(preparedStatementCreator(sql, params), PreparedStatement::executeUpdate);
    }

    /**
     * 只允许插入一条，然后返回该条主键
     * update and 主键返回
     *
     * @param sql
     * @param params
     * @return
     */
    public Number updateAndReturnGeneratedKey(String sql, Object... params) {
        return execute(
                (Connection con) -> {
                    //主键返回
                    var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    bindArgs(ps, params);
                    return ps;
                },
                (PreparedStatement ps) -> {
                    int n = ps.executeUpdate();
                    if (n == 0) {
                        throw new DataAccessException("0 rows inserted.");
                    }
                    if (n > 1) {
                        throw new DataAccessException("Multiple rows inserted.");
                    }
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        while (keys.next()) {
                            //直接返回第一个key
                            return (Number) keys.getObject(1);
                        }
                    }
                    throw new DataAccessException("Should not reach here.");
                }
        );
    }

    /**
     * @param preparedStatementCreator 预编译
     * @param action
     * @param <T>
     * @return
     */
    public <T> T execute(PreparedStatementCreator preparedStatementCreator, PreparedStatementCallback<T> action) {
        return execute((Connection con) -> {
            try (PreparedStatement ps = preparedStatementCreator.createPreparedStatement(con)) {
                return action.doInPreparedStatement(ps);
            }
        });
    }

    /**
     * @param sql
     * @param clazz 指定返回值class类型
     * @param args
     * @param <T>
     * @return
     * @throws DataAccessException
     */
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        if (clazz == String.class) {
            return (T) queryForObject(sql, StringRowMapper.INSTANCE, args);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) queryForObject(sql, BooleanRowNumber.INSTANCE, args);
        }
        //是否基本数据类型
        if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            return (T) queryForObject(sql, NumberRowMapper.INSTANCE, args);
        }
        //返回指定Bean
        //类似mybatis的resultType映射
        return queryForObject(sql, new BeanRowMapper<>(clazz), args);
    }

    /**
     * 获取指定Bean的结果集
     *
     * @param sql
     * @param clazz
     * @param args
     * @param <T>
     * @return
     * @throws DataAccessException
     */
    public <T> List<T> queryForList(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        //传入BeanRowMapper，映射Bean
        return queryForList(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(preparedStatementCreator(sql, args),
                (PreparedStatement ps) -> {
                    List<T> list = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rowMapper.mapRow(rs, rs.getRow()));
                        }
                    }
                    return list;
                });
    }

    /**
     * @param sql
     * @param rowMapper 接口，规定如何获取结果集
     * @param args
     * @param <T>
     * @return
     */
    private <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        //excute 使用Connection
        //preparedStatement预编译SQL，并且拼装参数
        return execute(preparedStatementCreator(sql, args),
                //PreparedStatementCallback
                (PreparedStatement ps) -> {
                    T t = null;
                    //执行查询逻辑，并结合PreparedStatement解析结果
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (t == null) {
                                //从结果集获取一条记录
                                t = rowMapper.mapRow(rs, rs.getRow());
                            } else {
                                throw new DataAccessException("Empty result set");
                            }
                        }
                    }
                    if (t == null) {
                        throw new DataAccessException("Empty result set");
                    }
                    return t;
                });
    }


    private PreparedStatementCreator preparedStatementCreator(String sql, Object... args) {
        return (Connection con) -> {
            //预编译SQL
            var ps = con.prepareStatement(sql);
            //绑定参数
            bindArgs(ps, args);
            return ps;
        };
    }

    /**
     * 为SQL设置参数
     *
     * @param ps
     * @param args
     * @throws SQLException
     */
    private void bindArgs(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            //按顺序设置SQL参数
            ps.setObject(i + 1, args[i]);
        }
    }


}


/**
 * 处理String类型结果
 */
class StringRowMapper implements RowMapper<String> {

    static StringRowMapper INSTANCE = new StringRowMapper();

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(1);
    }
}

/**
 * 处理Boolean类型结果
 */
class BooleanRowNumber implements RowMapper<Boolean> {

    static BooleanRowNumber INSTANCE = new BooleanRowNumber();

    @Nullable
    @Override
    public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBoolean(rowNum);
    }

}

/**
 * 处理Number类型结果
 */
class NumberRowMapper implements RowMapper<Number> {

    static NumberRowMapper INSTANCE = new NumberRowMapper();

    @Nullable
    @Override
    public Number mapRow(ResultSet rs, int rowNum) throws SQLException {
        return (Number) rs.getObject(rowNum);
    }


}
