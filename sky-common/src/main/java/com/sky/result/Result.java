package com.sky.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    private Integer code; //编码：1成功，0和其它数字为失败
    private String msg; //错误信息
    private T data; //数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.setCode(1);
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<T>();
        result.setData(object);
        result.setCode(1);
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result result = new Result();
        result.setMsg(msg);
        result.setCode(0);
        return result;
    }

}
