package com.xzn.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xzn.shortlink.admin.common.database.BaseDo;
import lombok.Data;

/**
 * @author Nruonan
 * 用户持久层实体
 */
@Data
@TableName("t_user")
public class UserDo extends BaseDo {


        /**
         * id
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
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

        /**
         * 注销时间戳
         */
        private Long deletionTime;


}
