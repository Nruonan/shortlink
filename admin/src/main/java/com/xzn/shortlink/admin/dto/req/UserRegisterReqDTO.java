package com.xzn.shortlink.admin.dto.req;

import lombok.Data;

/**
 * @author Nruonan
 *
 */
@Data
public class UserRegisterReqDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    private String password;
    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;

}
