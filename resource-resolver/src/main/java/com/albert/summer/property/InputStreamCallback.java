package com.albert.summer.property;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
@FunctionalInterface
public interface InputStreamCallback<T> {

    T doWithInputStream(InputStream stream) throws IOException;


}