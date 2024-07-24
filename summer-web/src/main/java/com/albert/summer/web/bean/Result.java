package com.albert.summer.web.bean;

/**
 * 不可变类
 * @param processed
 * @param returnObject
 */
public record Result(boolean processed, Object returnObject) {

}
