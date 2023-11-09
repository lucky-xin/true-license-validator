package com.license.validator.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 响应信息主体
 *
 * @param <T>
 * @author chaoxin.lu
 */
@Data
public class R<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 返回状态码 0: 成功标记， 1: 失败标记， 2: 没有数据
     */
    @Getter
    @Setter
    private int code;

    /**
     * 提示信息
     */
    @Getter
    @Setter
    private String msg = "success";

    /**
     * 数据
     */
    @Getter
    @Setter
    private T data;

    /**
     * 请求unique id
     */
    @Getter
    @Setter
    private String reqId;

    /**
     * 服务执行用时，单位毫秒
     */
    @Getter
    @Setter
    private Long took;
}
