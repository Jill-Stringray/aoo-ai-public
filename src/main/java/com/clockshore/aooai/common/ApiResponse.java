package com.clockshore.aooai.common;

import lombok.Builder;
import lombok.Data;

/**
 * 全站统一接口响应包装。
 * 具体业务只需要把自己的数据模型放到 data 字段里，例如 {@code ApiResponse<UserVO>}。</p>
 */
@Data
@Builder
public class ApiResponse<T> {

    /**
     * 请求在业务语义上是否成功。
     */
    private boolean success;

    /**
     * 程序可识别的业务码。
     */
    private String code;

    /**
     * 可展示给用户或用于调试的提示信息。
     */
    private String message;

    /**
     * 业务数据本体；失败时通常为空。
     */
    private T data;

    /**
     * 默认成功的方法
     * @param data
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 自定义成功的方法
     *
     * @param code
     * @param message
     * @param data
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 自定义失败的方法
     *
     * @param code
     * @param message
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }
}
