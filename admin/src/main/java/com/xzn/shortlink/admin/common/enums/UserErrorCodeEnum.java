package com.xzn.shortlink.admin.common.enums;

import com.xzn.shortlink.admin.common.convention.errorcode.IErrorCode;

/**
 * @author Nruonan
 * @description
 */
public enum UserErrorCodeEnum implements IErrorCode {
    USER_NULL("B0002000","用户记录不存在"),
    USER_EXIST("B000201","用户记录已存在");
    private final String code;

    private final String message;

    UserErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
