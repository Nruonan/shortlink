package com.xzn.shortlink.admin.dto.req;

import lombok.Data;

/**
 * @author Nruonan
 * @description
 */
@Data
public class UserUpdateReqDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;


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
