package com.albert.summer.property;

import jakarta.annotation.Nullable;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;


/**
 * 读取配置文件配置项和内容
 * 根据配置key获取配置内容
 *
 * @author yangjunwei
 * @date 2024/7/16
 */
public class PropertyResolver {

    Logger logger = Logger.getLogger(PropertyResolver.class.getName());

    Map<String, String> properties = new HashMap<>();

    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();


    /**
     * 读取配置文件
     *
     * @param properties
     */
    public PropertyResolver(Properties properties) {
        //环境变量
        this.properties.putAll(System.getenv());
        Set<String> names = properties.stringPropertyNames();
        //Properties
        for (String name : names) {
            this.properties.put(name, properties.getProperty(name));
        }

        //预设的类型转换器
        // String类型:
        converters.put(String.class, s -> s);
        // boolean类型:
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));
        // int类型:
        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));
        // 其他基本类型...
        // Date/Time类型:
        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));

    }

    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }

    /**
     * 根据key获取属性
     *
     * @param key
     * @return
     */
    @Nullable
    public String getProperty(String key) {
        //尝试解析${configValue:defaultValue}
        PropertyExpr propertyExpr = parsePropertyExpr(key);
        if (propertyExpr != null) {
            if (propertyExpr.defaultValue() != null) {
                return getProperty(propertyExpr.key(), propertyExpr.defaultValue());
            } else {
                return getRequiredProperty(propertyExpr.key());
            }
        }
        //从properties获取配置
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    public <T> T getProperty(String key, Class<T> clazz) {
        String property = getProperty(key);
        if (property == null) {
            return null;
        }
        return convert(clazz, property);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }


    /**
     * 类型转换
     * 预设的类型转换器
     *
     * @param clazz
     * @param value
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    /**
     * 解析值
     *
     * @param value
     * @return
     */
    private String parseValue(String value) {
        PropertyExpr propertyExpr = parsePropertyExpr(value);
        if (propertyExpr == null) {
            return value;
        }
        if (propertyExpr.defaultValue() != null) {
            return getProperty(propertyExpr.key(), propertyExpr.defaultValue());
        } else {
            return getRequiredProperty(propertyExpr.key());
        }
    }

    /**
     * 解析${configValue:defaultValue}
     *
     * @param key
     * @return
     */
    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            //defaultValue？
            int n = key.indexOf(":");
            if (n == (-1)) {
                //没有defaultVale,${value}
                String k = key.substring(2, key.length() - 1);
                return new PropertyExpr(k, null);
            } else {
                //有defaultValue,${value:default}
                String k = key.substring(2, n);
                String defaultValue = key.substring(n + 1, key.length() - 1);
                return new PropertyExpr(k, defaultValue);
            }
        }
        return null;
    }

    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }


}
