package com.albert.summer.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
public class YamlUtilsTest {

    @Test
    public void testParseYaml() {
        //解析yaml并扁平化处理
        Map<String, Object> configs = YamlUtils.loadYamlAsPlainMap("/application.yml");
        for (String key : configs.keySet()) {
            Object value = configs.get(key);
            System.out.println(key);
            System.out.println(key + ": " + value + " (" + value.getClass() + ")");
        }
        String title = configs.get("app.title").toString();
        System.out.println(title);

    }


}
