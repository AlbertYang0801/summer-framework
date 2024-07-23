package com.albert.summer.jdbc;

import com.albert.summer.exception.DataAccessException;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 从DB的row里面获取Bean
 * 根据Bean传进来的Class，自动找到Set方法然后组装Obj
 *
 * @author yangjunwei
 * @date 2024/7/23
 */
@Slf4j
public class BeanRowMapper<T> implements RowMapper<T> {

    Class<T> clazz;

    Constructor<T> constructor;

    Map<String, Field> fields = new HashMap<>();
    Map<String, Method> methods = new HashMap<>();

    /**
     * 注入目标类的方法和字段信息
     * @param clazz
     */
    public BeanRowMapper(Class<T> clazz) {
        this.clazz = clazz;
        try {
            this.constructor = clazz.getConstructor();

        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("No public default constructor found for class %s when build BeanRowMapper.", clazz.getName()), e);
        }
        for (Field field : clazz.getFields()) {
            //方法名称
            String name = field.getName();
            this.fields.put(name, field);
            log.debug("Add row mapping: {} to field {}", name, name);
        }

        //set方法
        for (Method method : clazz.getMethods()) {
            Parameter[] parameters = method.getParameters();
            if (parameters.length == 1) {
                String name = method.getName();
                if (name.length() >= 4 && name.startsWith("set")) {
                    String prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    this.methods.put(prop, method);
                    log.debug("Add row mapping: {} to {}({})", prop, name, parameters[0].getType().getSimpleName());
                }
            }
        }
    }

    /**
     * 根据DB SQL的返回结果通过反射设置Bean
     * SQL里面的结果，可以映射到Obj的field和set方法
     * @param rs
     * @param rowNum
     * @return
     * @throws SQLException
     */
    @Nullable
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T bean;
        try {
           bean = this.constructor.newInstance();
            ResultSetMetaData metaData = rs.getMetaData();
            //字段数量
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                //根据sql别名获取字段信息
                String columnLabel = metaData.getColumnLabel(i);
                Method method = this.methods.get(columnLabel);
                if(method != null) {
                    //从SQL查询结果集获取真实值
                    //执行方法，入参为DB返回字段对应值
                    method.invoke(bean,rs.getObject(columnLabel));
                }else{
                    Field field = this.fields.get(columnLabel);
                    if(field != null) {
                        //为字段设置属性
                        field.set(bean,rs.getObject(columnLabel));
                    }
                }
            }
        }catch (ReflectiveOperationException e){
            throw new DataAccessException(String.format("Could not map result set to class %s", this.clazz.getName()), e);
        }
        return bean;
    }


}
