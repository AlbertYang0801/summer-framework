package com.albert.summer.web.bean;

/**
 * 参数类型
 * @author yangjunwei
 * @date 2024/7/24
 */
public enum ParamType {

    /**
     * 对应@PthVariable
     */
    PATH_VARIABLE,
    /**
     * 对应@RequestParam
     */
    REQUEST_PARAM,
    /**
     * 对应@RequstBody
     */
    REQUEST_BODY,
    /**
     * 服务本身提供，比如HttpServletRequest
     */
    SERVICE_VARIABLE;


}
